package mobi.upod.app.services.playback

import java.net.URL
import java.util.{Timer, TimerTask}

import android.content.{Context, Intent}
import android.graphics.SurfaceTexture
import android.media.AudioManager
import android.media.AudioManager.OnAudioFocusChangeListener
import mobi.upod.android.app.{BoundService, IntegratedNotificationManager, ServiceBinder}
import mobi.upod.android.content.preferences.PreferenceChangeListener
import mobi.upod.android.logging.Logging
import mobi.upod.android.os.AsyncTask
import mobi.upod.android.util.ApiLevel
import mobi.upod.app.data.{EpisodeBaseWithDownloadInfo, EpisodeBaseWithPlaybackInfo, EpisodeListItem}
import mobi.upod.app.gui.playback.{PlaybackActivity, PlaybackErrorActivity}
import mobi.upod.app.services.EpisodeNotificationBuilder
import mobi.upod.app.services.playback.PlaybackServiceImpl.PlaybackNotificationBuilder
import mobi.upod.app.services.playback.player.SwitchableMediaPlayer
import mobi.upod.app.services.playback.state._
import mobi.upod.app.storage.PlaybackNotificationButtons._
import mobi.upod.app.storage.PlaybackPreferences
import mobi.upod.app.{AppInjection, R}
import mobi.upod.media.{MediaChapter, MediaChapterTable}
import mobi.upod.util.Duration._
import mobi.upod.util.Permille._

class PlaybackServiceImpl
  extends BoundService[PlaybackController]
  with PlaybackController
  with PlaybackListener
  with StateMachine
  with IntegratedNotificationManager
  with AppInjection
  with OnAudioFocusChangeListener
  with Logging {

  private lazy val playlistService = inject[PlaybackService]
  private lazy val audioManager = inject[AudioManager]
  private lazy val playbackPreferences = inject[PlaybackPreferences]
  protected[playback] lazy val player = new SwitchableMediaPlayer(this)

  private lazy val bufferingNotification = createBufferingNotificationBuilder
  private var _playbackNotification: Option[EpisodeNotificationBuilder] = None
  private var _pauseNotification: Option[EpisodeNotificationBuilder] = None
  private lazy val mediaSession = new MediaSession(this)
  private var _sleepTimerMode: SleepTimerMode = SleepTimerMode.Off
  private var sleepTimer: Option[SleepTimer] = None

  addSynchronousListener(this)

  //
  // lifecycle management
  //

  override def onCreate(): Unit = {
    super.onCreate()
    playbackPreferences.notificationButtons.addWeakListener(ButtonConfigChangeListener)
  }

  override def onDestroy(): Unit = {
    playbackPreferences.notificationButtons.removeListener(ButtonConfigChangeListener)
    player.release()
    mediaSession.release()
    log.info("destroying playback service")
    super.onDestroy()
  }

  override def onTaskRemoved(rootIntent: Intent): Unit = {
    log.info("uPod task removed -- stopping playback")
    super.onTaskRemoved(rootIntent)
    if (canResume || canPlay) {
      stop()
      stopSelf()
    }
  }

  //
  // states
  //

  def idle = is[Idle] || is[Completed]

  def playing = is[Playing]

  def paused = is[Paused]

  def canPlay = is[Playable]

  def canResume = is[Resumable]

  def canPause = is[Pausable]

  def canSeek = is[Seekable] && player.canSeek

  def canSkipChapter = stateAs[Seekable].exists(_.canSkipChapter)

  def canGoBackChapter = stateAs[Seekable].exists(_.canGoBackChpter)

  def canStop = is[Stoppable]

  def episode: Option[EpisodeListItem] = state match {
    case s: StateWithEpisode => Some(s.episode)
    case _ => None
  }

  def playingEpisode: Option[EpisodeListItem] =
    if (is[Playing] || is[Paused] || is[Preparing]) episode else None

  def chapters: Option[MediaChapterTable] =
    stateAs[Seekable].map(_.chapters)

  def position: Long =
    stateAs[StateWithPlaybackPosition].map(_.playbackPosition.toLong).
    orElse(stateAs[StateWithEpisode].map(_.episode.playbackInfo.playbackPosition)).
    getOrElse(0)

  def currentChapter: Option[MediaChapter] =
    stateAs[Seekable].flatMap(_.currentChapter)

  def videoSize =
    if (is[Playing] || is[Paused]) player.getVideoSize else VideoSize(0, 0)

  def play(episodeId: Long): Unit =
    loadEpisodeAnd(_.play(_), episodeDao.findListItemById(episodeId))

  override def joinRemoteSession(mediaUrl: URL, state: RemotePlaybackState.RemotePlaybackState): Unit = {
    log.info(s"trying to join remote playback for url $mediaUrl in state $state")
    loadEpisodeAnd(_.joinRemoteSession(_, state), episodeDao.findListItemByUrl(mediaUrl), forceStop(false))
  }

  private def loadEpisodeAnd(playEpisode: (Playable, EpisodeListItem) => Unit, findEpisode: => Option[EpisodeListItem], onUnknownEpisode: => Unit = forceStop()): Unit = {
    log.info(s"loading episode...")
    AsyncTask.execute {
      val episode = findEpisode
      episode.foreach(e => playlistService.insertEpisodeAtStart(e.episodeId))
      episode
    } {
      case Some(e) =>
        log.info(s"loaded episode ${e.uri} with playback position ${e.playbackInfo.playbackPosition}")
        reset()
        if (is[Playable]) {
          ifIs[Playable](playEpisode(_, e))
        }
      case _ =>
        log.info(s"haven't found requested episode; stopping")
        onUnknownEpisode
    }
  }

  def pause() {
    ifIs[Pausable](_.pause())
  }

  def resume() {
    ifIs[Paused](_.resume())
  }

  def fastForward() {
    seekRelative(playbackPreferences.fastForwardSeconds.get.seconds)
  }

  def rewind() {
    seekRelative(-playbackPreferences.rewindSeconds.get.seconds)
  }

  def seek(position: Long, commit: Boolean) {
    ifIs[Seekable](_.seek(position.toInt, commit))
  }

  def seekRelative(offset: Int) {
    ifIs[Seekable](_.seek(player.getCurrentPosition + offset, true))
  }

  override def skipChapter(): Unit = ifIs[Seekable] { seekable =>
    val skippedChapter = seekable.skipChapter()
    if (!skippedChapter) skip()
  }

  override def goBackChapter(): Unit = ifIs[Seekable] { seekable =>
    val wentBack = seekable.goBackChapter()
    if (!wentBack) seekable.seek(1, true)
  }

  def skip() {
    ifIs[Playing](_.pause())
    ifIs[StateWithEpisode] { state =>
      AsyncTask.execute {
        playlistService.markEpisodeFinished(state.episode.id)
        playNextOrStop()
      }
    }
  }

  private def playNextOrStop(): Unit = {
    playlistService.findFirstPlayablePlaylistEpisodeId match {
      case Some(e) => play(e)
      case None => stop()
    }
  }

  def stop() {
    forceStop()
  }

  private def fadeOut(): Unit = ifIs[Playing](_.fadeOut())

  def getSurface = player.getSurface

  def setSurface(surface: Option[SurfaceTexture]) = if (player.isPlayerSet) {
    player.setSurface(surface)
  }

  def setCareForSurface(care: Boolean) =
    player.setCareForSurface(care)

  def audioFxAvailability =
    player.audioFxAvailability

  def setPlaybackSpeedMultiplier(multiplier: Float) =
    player.setPlaybackSpeedMultiplier(multiplier)

  def playbackSpeedMultiplier =
    player.playbackSpeedMultiplier

  override def volumeGain: Float =
    player.volumeGain

  override def setVolumeGain(gain: Float): Unit =
    player.setVolumeGain(gain)

  override def sleepTimerMode: SleepTimerMode =
    _sleepTimerMode

  override def startSleepTimer(mode: SleepTimerMode): Unit =
    setSleepTimerMode(mode)

  override def cancelSleepTimer(): Unit =
    setSleepTimerMode(SleepTimerMode.Off)

  //
  // state handling
  //

  protected def onStateChanged(oldState: PlaybackState, newState: PlaybackState) {
    newState match {
      case state: Idle =>
      case state: Buffering =>
        fire(_.onEpisodeChanged(Some(state.episode)))
        fire(_.onPreparingPlayback(state.episode))
      case state: Preparing =>
        fire(_.onEpisodeChanged(Some(state.episode)))
        fire(_.onPreparingPlayback(state.episode))
      case state: Playing => fire(_.onPlaybackStarted(state.episode))
      case state: FadingOut => fire(_.onPreparingPlayback(state.episode))
      case state: Paused => fire(_.onPlaybackPaused(state.episode))
      case state: FocusLostTransiently =>
      case state: Stopped => fire(_.onPlaybackStopped())
      case state: Completed =>
    }
  }

  def onBufferingStarted(episode: EpisodeBaseWithDownloadInfo) {
    startForeground(bufferingNotification.
                    setEpisode(episode).
                    setIndeterminateProgress())
  }

  def onBufferingProgress(episode: EpisodeBaseWithDownloadInfo, bufferedMillis: Long, requiredMillis: Long) {
    updateNotification(bufferingNotification.setProgress(bufferedMillis.toInt, requiredMillis.toInt))
  }

  def onBufferingStopped(episode: EpisodeBaseWithDownloadInfo) {
    stopForeground()
  }

  override def onEpisodeChanged(episode: Option[EpisodeBaseWithPlaybackInfo]): Unit =
    episode.foreach(setNotificationEpisode)

  override def onPlaybackPositionChanged(episode: EpisodeBaseWithPlaybackInfo) {
    setNotificationProgress(episode)
    if (playing)
      updateNotification(playbackNotification)
    else if (paused)
      updateNotification(pauseNotification)
  }

  override def onPreparingPlayback(episode: EpisodeBaseWithPlaybackInfo) {
    stopForeground()
    setNotificationProgress(episode)
    startForeground(playbackNotification.setIndeterminateProgress())
  }

  override def onPlaybackStarted(episode: EpisodeBaseWithPlaybackInfo) {
    setNotificationProgress(episode)
    updateNotification(playbackNotification.setProgress(0, 1000))
  }

  override def onPlaybackPaused(episode: EpisodeBaseWithPlaybackInfo) {
    setNotificationProgress(episode)
    updateNotification(pauseNotification)
  }

  override def onEpisodeCompleted(episode: EpisodeBaseWithPlaybackInfo) {
    val stopOnEpisodeEnd = sleepTimer.exists(_.stopsOnEpisodeEnd)
    if (stopOnEpisodeEnd) {
      sleepTimer.foreach(_.done())
    }
    AsyncTask.execute {
      playlistService.removeEpisode(episode)
      if (stopOnEpisodeEnd)
        stop()
      else
        playNextOrStop()
    }
  }

  override def onPlaybackStopped() {
    cancelSleepTimer()
    stopForeground()
  }

  private def startForeground(notification: EpisodeNotificationBuilder) {
    resetErrorNotification()
    startForeground(notification.build)
    requestAudioFocus()
    mediaSession.register()
  }

  override protected def stopForeground() {
    mediaSession.unregister()
    abandonAudioFocus()
    super.stopForeground()
  }

  //
  // remote episode change
  //

  override def onRemoteEpisodeChanged(mediaUrl: URL, state: RemotePlaybackState.RemotePlaybackState): Unit =
    joinRemoteSession(mediaUrl, state)

  //
  // audio focus
  //

  protected def requestAudioFocus(): Boolean = {
    log.info("requesting audio focus")
    audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN) ==
      AudioManager.AUDIOFOCUS_REQUEST_GRANTED
  }

  protected def abandonAudioFocus() {
    log.info("abandon audio focus")
    audioManager.abandonAudioFocus(this)
  }

  def onAudioFocusChange(focusChange: Int) {
    focusChange match {
      case AudioManager.AUDIOFOCUS_GAIN =>
        log.info("gained audio focus")
        ifIs[AudioFocusable](_.onAudioFocusGranted())
      case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT | AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK =>
        log.info("transiently lost audio focus")
        ifIs[AudioFocusable](_.onAudioFocusLostTransient())
      case AudioManager.AUDIOFOCUS_LOSS =>
        log.info("permanently lost audio focus")
        pause()
    }
  }

  //
  // error
  //

  private def resetErrorNotification() {
    notificationManager.cancel(R.string.playback_error)
  }

  protected def onPlaybackError(episode: Option[EpisodeListItem], error: PlaybackError): Unit = {
    stopForeground()
    fire(_.onPlaybackStopped())
    val errorNotification = new EpisodeNotificationBuilder(this, false).
      setAutoCancel().
      setIcon(R.drawable.ic_stat_error).
      setActivity(PlaybackErrorActivity.intent(getApplicationContext, error, episode)).
      setContentText(getString(R.string.playback_error_content))
    episode.foreach(errorNotification.setEpisode)
    notificationManager.notify(R.string.playback_error, errorNotification)
  }

  //
  // Observable
  //

  protected def fireActiveState(listener: PlaybackListener) {
    listener.onEpisodeChanged(episode)
    state match {
      case state: Idle =>
      case state: Buffering =>
        listener.onPreparingPlayback(state.episode)
      case state: Preparing =>
        listener.onPreparingPlayback(state.episode)
      case state: Playing =>
        listener.onPlaybackStarted(state.episode)
      case state: FadingOut =>
        listener.onPreparingPlayback(state.episode)
      case state: Paused =>
        listener.onPlaybackPaused(state.episode)
      case state: Stopped =>
        listener.onPlaybackStopped()
      case state: Completed =>
    }
    if (_sleepTimerMode != SleepTimerMode.Off) {
      listener.onSleepTimerModeChanged(_sleepTimerMode)
    }
  }

  override def fire(event: (PlaybackListener) => Unit) {
    super.fire(event)
  }

  //
  // notification handling
  //

  implicit private def playbackNotificationBuilder(builder: EpisodeNotificationBuilder): PlaybackNotificationBuilder =
    new PlaybackNotificationBuilder(builder)

  private def addNotificationActions(
    builder: EpisodeNotificationBuilder,
    buttonConfig: PlaybackNotificationButtons,
    addPrimaryAction: EpisodeNotificationBuilder => EpisodeNotificationBuilder): EpisodeNotificationBuilder = {

    val ctx = getApplicationContext

    def addStopAction(): EpisodeNotificationBuilder =
      builder.addPlaybackAction(ctx, R.id.action_media_stop, R.drawable.ic_action_stop, R.string.action_media_stop)

    def addSkipAction(): EpisodeNotificationBuilder =
      builder.addPlaybackAction(ctx, R.id.action_media_skip, R.drawable.ic_action_skip, R.string.action_media_skip)

    def addRewindAction(): EpisodeNotificationBuilder = builder.addPlaybackAction(
      ctx,
      R.id.action_media_rewind,
      R.drawable.ic_action_fast_rewind,
      ctx.getString(R.string.action_media_rewind, playbackPreferences.rewindSeconds.get: java.lang.Integer))

    def addFastForwardAction(): EpisodeNotificationBuilder = builder.addPlaybackAction(
      ctx,
      R.id.action_media_fast_forward,
      R.drawable.ic_action_fast_forward,
      ctx.getString(R.string.action_media_fast_forward, playbackPreferences.fastForwardSeconds.get: java.lang.Integer))

    if (ApiLevel >= ApiLevel.Lollipop) {
      addStopAction()
      addRewindAction()
      addPrimaryAction(builder)
      addFastForwardAction()
      addSkipAction()
      builder.setMediaStyle(mediaSession.token, 1, 2, 3)
    } else {
      buttonConfig match {
        case StopPlaySkip =>
          addStopAction()
          addPrimaryAction(builder)
          addSkipAction()
        case StopPlayFastForward =>
          addStopAction()
          addPrimaryAction(builder)
          addFastForwardAction()
        case RewindPlayFastForward =>
          addRewindAction()
          addPrimaryAction(builder)
          addFastForwardAction()
      }
    }
  }

  private def createBaseNotificationBuilder(icon: Int): EpisodeNotificationBuilder = {
    new EpisodeNotificationBuilder(this).
      setIcon(icon).
      setActivity(PlaybackActivity.intent(getApplicationContext))
  }

  private def createBufferingNotificationBuilder: EpisodeNotificationBuilder = {
    createBaseNotificationBuilder(R.drawable.ic_stat_playing).
      addPlaybackAction(getApplicationContext, R.id.action_media_stop, R.drawable.ic_action_stop, R.string.action_media_stop).
      setContentText(getString(R.string.buffering))
  }

  private def createPlaybackNotificationBuilder(buttonConfig: PlaybackNotificationButtons): EpisodeNotificationBuilder = {
    val builder = createBaseNotificationBuilder(R.drawable.ic_stat_playing)
    addNotificationActions(
      builder,
      buttonConfig,
      _.addPlaybackAction(getApplicationContext, R.id.action_media_pause, R.drawable.ic_action_pause_simple, R.string.action_media_pause))
    ifIs[StateWithEpisode](state => builder.setEpisode(state.episode))
    builder
  }

  private def createPauseNotificationBuilder(buttonConfig: PlaybackNotificationButtons): EpisodeNotificationBuilder = {
    val builder = createBaseNotificationBuilder(R.drawable.ic_stat_paused)
    addNotificationActions(
      builder,
      buttonConfig,
      _.addPlaybackAction(getApplicationContext, R.id.action_media_resume, R.drawable.ic_action_play_simple, R.string.action_media_play))
  }

  private def playbackNotification: EpisodeNotificationBuilder = _playbackNotification match {
    case Some(notification) =>
      notification
    case None =>
      val notification = createPlaybackNotificationBuilder(playbackPreferences.notificationButtons)
      _playbackNotification = Some(notification)
      notification
  }

  private def pauseNotification: EpisodeNotificationBuilder = _pauseNotification match {
    case Some(notification) =>
      notification
    case None =>
      val notification = createPauseNotificationBuilder(playbackPreferences.notificationButtons)
      _pauseNotification = Some(notification)
      notification
  }

  private def setNotificationEpisode(episode: EpisodeBaseWithPlaybackInfo): Unit = {
    bufferingNotification.setEpisode(episode)
    playbackNotification.setEpisode(episode)
    pauseNotification.setEpisode(episode)
  }

  private def setNotificationProgress(episode: EpisodeBaseWithPlaybackInfo) {

    def setProgress(notification: EpisodeNotificationBuilder, contentText: Int) {
      val progress = episode.playbackInfo.playbackPosition.permille(episode.media.duration).toInt
      notification.setProgress(progress, PermilleMax)

      val mediaTime = episode.formattedPosition(playbackPreferences.mediaTimeFormat, true)
      notification.setContentText(getString(contentText, mediaTime))
    }

    setProgress(playbackNotification, R.string.playing)
    setProgress(pauseNotification, R.string.paused)
  }

  private object ButtonConfigChangeListener extends PreferenceChangeListener[PlaybackNotificationButtons] {

    override def onPreferenceChange(newValue: PlaybackNotificationButtons): Unit = {
      _playbackNotification = Some(createPlaybackNotificationBuilder(newValue))
      _pauseNotification = Some(createPauseNotificationBuilder(newValue))

      ifIs[StateWithEpisode] { state =>
        setNotificationEpisode(state.episode)
        setNotificationProgress(state.episode)
      }
      ifIs[Paused](_ => updateNotification(_pauseNotification.get))
    }
  }

  //
  // playback timer
  //

  private def setSleepTimerMode(mode: SleepTimerMode): Unit = synchronized {
    if (mode != _sleepTimerMode) {
      _sleepTimerMode = mode
      installSleepTimer(mode)
      fire(_.onSleepTimerModeChanged(mode))
    }
  }

  private def installSleepTimer(mode: SleepTimerMode): Unit = synchronized {

    def uninstallSleepTimer(): Unit = synchronized {
      sleepTimer foreach { timer =>
        log.info(s"uninstalled SleepTimer of class ${timer.getClass.getName}")
        removeListener(timer)
        timer.destroy()
      }
    }

    uninstallSleepTimer()
    sleepTimer = mode match {
      case SleepTimerMode.Chapter => Some(new EndOfChapterSleepTimer)
      case SleepTimerMode.Episode => Some(new EndOfEpisodeSleepTimer)
      case m: SleepTimerMode.Timer => Some(new TimedSleepTimer(m))
      case _ => None
    }
    sleepTimer foreach { timer =>
      log.info(s"installed SleepTimer of class ${timer.getClass.getName}")
      addListener(timer, false)
    }
  }

  private trait SleepTimer extends PlaybackListener {
    private var isDone = false

    def destroy(): Unit = ()

    def stopsOnEpisodeEnd: Boolean = false

    def done(): Unit = synchronized {
      if (!isDone) {
        isDone = true
        log.info(s"SleepTimer of type ${getClass.getName} is done")
        setSleepTimerMode(SleepTimerMode.Off)
      }
    }
  }

  private class EndOfEpisodeSleepTimer extends SleepTimer {

    override def stopsOnEpisodeEnd: Boolean = true
  }

  private final class EndOfChapterSleepTimer extends EndOfEpisodeSleepTimer {

    override def stopsOnEpisodeEnd: Boolean = true

    override def onCurrentChapterChanged(chapter: Option[MediaChapter]): Unit = {
      done()
      pause()
    }
  }

  private final class TimedSleepTimer(timedMode: SleepTimerMode.Timer) extends SleepTimer {
    private var timer: Option[Timer] = None

    init()

    private def init(): Unit = {
      ifNotIs[Playing](startTimer())
    }

    override def destroy(): Unit =
      cancelTimer()

    override def onPlaybackPositionChanged(episode: EpisodeBaseWithPlaybackInfo): Unit =
      doneIfExpired()

    override def onPlaybackPaused(episode: EpisodeBaseWithPlaybackInfo): Unit = {
      // in paused mode we will not received updated playback positions, wo we have to care for a timer event by ourselves
      startTimer()
    }

    override def onPlaybackStarted(episode: EpisodeBaseWithPlaybackInfo): Unit =
      cancelTimer()

    private def startTimer(): Unit = synchronized {
      if (timer.isEmpty) {
        val t = new Timer("SleepTimer")
        timer = Some(t)
        t.schedule(TimedTask, timedMode.remaining + 100)
      }
    }

    private def cancelTimer(): Unit = synchronized {
      timer.foreach(_.cancel())
    }

    private def doneIfExpired(): Unit = if (timedMode.isFinished) {
      done()
      fadeOut()
    }

    private object TimedTask extends TimerTask {

      override def run(): Unit = doneIfExpired()
    }
  }
}

object PlaybackServiceImpl extends ServiceBinder[PlaybackController, PlaybackServiceImpl] {

  private[playback] class PlaybackNotificationBuilder(val builder: EpisodeNotificationBuilder) extends AnyVal {

    def addPlaybackAction(context: Context, actionId: Int, drawable: Int, title: Int): EpisodeNotificationBuilder =
      addPlaybackAction(context, actionId, drawable, context.getString(title))

    def addPlaybackAction(context: Context, actionId: Int, drawable: Int, title: String): EpisodeNotificationBuilder = {
      builder.addAction(drawable, title, RemotePlaybackActionReceiver(context, actionId))
      builder
    }
  }
}