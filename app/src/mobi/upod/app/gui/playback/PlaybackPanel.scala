package mobi.upod.app.gui.playback

import android.app.Activity
import android.content.Context
import android.support.v4.app.ActivityOptionsCompat
import android.view.View
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.{SeekBar, Button, ImageView, TextView}
import com.escalatesoft.subcut.inject.Injectable
import mobi.upod.android.app.action.{Action, ActionController}
import mobi.upod.android.app.{ActivityLifecycleListener, SimpleAlertDialogFragment}
import mobi.upod.android.logging.Logging
import mobi.upod.android.os.AsyncTask
import mobi.upod.android.view.{ChildViews, Tintable}
import mobi.upod.android.widget.{TintableSeekBar, ActionButtons, FloatingActionButton}
import mobi.upod.app.R
import mobi.upod.app.data._
import mobi.upod.app.gui.{CoverartLoader, CoverartLoaderFallbackDrawable, CoverartPlaceholderDrawable, Theme}
import mobi.upod.app.services.download.{DownloadListener, DownloadService}
import mobi.upod.app.services.licensing.LicenseService
import mobi.upod.app.services.playback._
import mobi.upod.app.services.sync.{SyncListener, SyncService}
import mobi.upod.app.services.{EpisodeListener, EpisodeService}
import mobi.upod.app.storage.{EpisodeDao, ImageSize, PlaybackPreferences}
import mobi.upod.media.MediaChapterTable
import mobi.upod.util.MediaPositionFormat._
import mobi.upod.util.Permille._
import mobi.upod.util.{MediaFormat, MediaPosition}

trait PlaybackPanel
  extends ActivityLifecycleListener
  with ChildViews
  with PlaybackListener
  with DownloadListener
  with SyncListener
  with EpisodeListener
  with ActionButtons
  with ActionController
  with Injectable
  with Logging {

  protected lazy val episodeDao = inject[EpisodeDao]
  private lazy val downloadService = inject[DownloadService]
  protected lazy val playbackService = inject[PlaybackService]
  protected lazy val episodeService = inject[EpisodeService]
  protected lazy val syncService = inject[SyncService]
  protected lazy val licenseService = inject[LicenseService]
  protected lazy val playbackPreferences = inject[PlaybackPreferences]
  private lazy val coverartLoader = inject[CoverartLoader]

  protected def createActions: Map[Int, Action] = Map(
    R.id.action_media_stop -> new StopPlaybackAction,
    R.id.action_media_pause -> new PausePlaybackAction,
    R.id.action_media_resume -> new ResumePlaybackAction,
    R.id.action_media_skip -> new SkipEpisodePlaybackAction,
    R.id.action_media_fast_forward -> new FastForwardPlaybackAction with SeekActionObserver,
    R.id.action_media_rewind -> new RewindPlaybackAction with SeekActionObserver
  )

  protected lazy val theme = new Theme(getActivity)
  protected lazy val playbackPanel = childViewGroup(R.id.playbackPanel)
  protected lazy val playbackControl = playbackPanel.childViewGroup(R.id.playbackControl)
  protected lazy val preparingPlaybackIndicator = playbackControl.childProgressBar(R.id.preparingPlaybackIndicator)
  protected lazy val podcastImageView = playbackControl.optionalChildAs[ImageView](R.id.podcastImage)
  protected lazy val podcastTitleView = playbackControl.optionalChildAs[TextView](R.id.podcastTitle)
  protected lazy val episodeTitleView = playbackControl.optionalChildAs[TextView](R.id.episodeTitle)
  protected lazy val mediaPositionView = playbackControl.optionalChildAs[TextView](R.id.mediaPosition)
  protected lazy val mediaDurationView = playbackControl.optionalChildAs[TextView](R.id.mediaDuration)
  protected lazy val mediaTimeView = playbackControl.optionalChildAs[TextView](R.id.mediaTime)
  protected lazy val mediaProgressView = playbackControl.childProgressBar(R.id.mediaProgress)
  protected lazy val mediaPauseButton = playbackControl.childView(R.id.action_media_pause)
  protected lazy val mediaPlayButton = playbackControl.childView(R.id.action_media_resume)
  protected lazy val mediaInfo = playbackControl.optionalChildAs[View](R.id.mediaInfo)
  protected lazy val playbackSpeedIndicator = playbackControl.optionalChildAs[PlaybackSpeedIndicatorView](R.id.playbackSpeed)
  protected lazy val volumeGainIndicator = playbackControl.optionalChildAs[VolumeGainIndicator](R.id.volumeGain)
  protected lazy val sleepTimerIndicator = playbackControl.optionalChildAs[SleepTimerController](R.id.sleepTimer)
  protected lazy val coverartPlaceholderDrawable = new CoverartPlaceholderDrawable with CoverartLoaderFallbackDrawable

  protected val podcastImageSize = ImageSize.list

  private var _episode: Option[EpisodeListItem] = None

  protected def episode: Option[EpisodeListItem] = _episode

  protected def episode_=(e: Option[EpisodeListItem]): Unit =
    _episode = e

  protected def createPlaybackPanel(): Unit = {
    initActionButtons(playbackControl)
    mediaInfo.foreach(_.onClick(onMediaInfoClick()))
    mediaProgressView.setMax(PermilleMax)
    mediaPositionView.foreach(_.onClick(toggleMediaPositionFormat()))
    mediaDurationView.foreach(_.onClick(toggleMediaDurationFormat()))
    playbackSpeedIndicator.foreach(_.onClick(onPlaybackSpeedIndicatorClicked()))
    playbackSpeedIndicator.foreach(_.makeInvisible(true))
    volumeGainIndicator foreach { vgi =>
      vgi.onClick(onVolumeGainIndicatorClicked())
      vgi.makeInvisible(true)
    }
    sleepTimerIndicator foreach { sti =>
      sti.setActivity(getActivity)
      sti.makeInvisible(true)
    }
  }

  override def onActivityStart() {
    playbackService.addWeakListener(this)
    downloadService.addWeakListener(this)
    syncService.addWeakListener(this)
    sleepTimerIndicator foreach { sti =>
      sti.setMode(playbackService.sleepTimerMode)
      sti.becomingVisible()
    }
    invalidateActionButtons()
  }

  override def onActivityStop() {
    sleepTimerIndicator.foreach(_.becomingInvisible())
    syncService.removeListener(this)
    downloadService.removeListener(this)
    playbackService.removeListener(this)
    episodeService.removeListener(this)
  }

  protected def onMediaInfoClick(): Unit = {
    showPlayback()
  }

  override def invalidateActionButtons(): Unit = {
    super.invalidateActionButtons()

    preparingPlaybackIndicator.show(!playbackService.canResume && !playbackService.canPause)
    sleepTimerIndicator foreach { sti =>
      val visible = licenseService.isLicensed && (playbackService.isPaused || playbackService.isPlaying)
      sti.makeInvisible(!visible)
    }
  }

  protected def tintViews(episode: EpisodeListItem): Unit = {
    val keyColor = episode.extractedOrGeneratedColors.key.getOrElse(theme.Colors.Primary)
    Tintable.tintOrIgnore(mediaProgressView, keyColor)
    playbackSpeedIndicator.foreach(_.setTint(theme.Colors.White))
    volumeGainIndicator.foreach(_.setTint(theme.Colors.White))
    sleepTimerIndicator.foreach(_.setTint(theme.Colors.White))
    mediaPauseButton match {
      case btn: Button => Tintable.tint(btn.getCompoundDrawables()(0), keyColor)
      case btn: FloatingActionButton => btn.setColor(keyColor)
    }
    mediaPlayButton match {
      case btn: FloatingActionButton => btn.setColor(keyColor)
      case _ => // ignore
    }
  }

  //
  // show playback view
  //

  def getActivity: Activity

  private def showPlayback(): Unit = {
    //val extras = ActivityOptionsCompat.makeSceneTransitionAnimation(getActivity, podcastImageView, "podcastImage").toBundle
    val extras = ActivityOptionsCompat.makeScaleUpAnimation(playbackPanel, 0, 0, playbackPanel.getWidth, playbackPanel.getHeight).toBundle
    PlaybackActivity.start(getActivity, episode, Some(extras))
  }

  //
  // playback listener
  //

  protected def positionUpdateAllowed: Boolean = true

  protected def showEpisode(episode: EpisodeListItem): Unit = if (episode != _episode.orNull) {
    _episode = Some(episode)
    showControls(true)
    podcastTitleView.foreach(v => v.setText(episode.podcastInfo.title))
    episodeTitleView.foreach(_.setText(episode.title))
    coverartPlaceholderDrawable.set(episode.podcastInfo.title, episode.extractedOrGeneratedColors)
    podcastImageView.foreach(coverartLoader.displayImage(_, podcastImageSize, episode.podcastInfo.imageUrl, Some(coverartPlaceholderDrawable)))
    tintViews(episode)
    onPlaybackPositionChanged(episode)
    onDownloadProgressChanged(episode)
    invalidateActionButtons()
  }

  //
  // empty actions
  //

  protected def showControls(show: Boolean): Unit =
      playbackControl.show(show)

  //
  // playback listener
  //

  override def onEpisodeChanged(episode: Option[EpisodeBaseWithPlaybackInfo]) {
    log.debug(s"episode changed to ${episode.map(_.getClass.getSimpleName)}")
    episodeService.addWeakListener(this, false)
    episode match {
      case Some(e) => e match {
        case ep: EpisodeListItem => showEpisode(ep)
        case ep => AsyncTask.execute(episodeDao.findListItemById(ep.id))(_.foreach(showEpisode))
      }
      case None =>
        _episode = None
        showControls(false)
    }
    updateFxIndicatorStatus()
  }

  override def onChaptersChanged(chapters: MediaChapterTable): Unit =
    sleepTimerIndicator.foreach(_.setHasChapter(!chapters.isEmpty))

  override def onPlaybackPositionChanged(episode: EpisodeBaseWithPlaybackInfo) {
    _episode = _episode.map {e =>
      e.copy(
        media = e.media.copy(duration = episode.media.duration),
        playbackInfo = e.playbackInfo.copy(playbackPosition = episode.playbackInfo.playbackPosition))
    }
    if (positionUpdateAllowed) {
      updatePlaybackPosition()
    }
  }

  private def updatePlaybackPosition(): Unit = _episode.foreach { episode =>
    updatePlaybackPosition(episode.playbackInfo.playbackPosition, episode.media.duration)

    val format = playbackPreferences.mediaTimeFormat
    mediaTimeView.foreach(_.setText(episode.formattedPosition(format, true)))
  }

  protected def updatePlaybackPosition(position: Long, duration: Long): Unit = {
    val pos = MediaPosition(position, duration)
    val format = playbackPreferences.mediaTimeFormat
    mediaPositionView.foreach(_.setText(MediaFormat.formatPosition(pos, format, true)))
    mediaDurationView.foreach(_.setText(MediaFormat.formatDuration(pos, format, true)))
    mediaProgressView.setProgress(position.permille(duration).toInt)
  }

  private def onDownloadProgressChanged(episode: EpisodeBaseWithDownloadInfo) {
    _episode = _episode.map {e =>
      e.copy(
        media = e.media.copy(length = episode.media.length),
        downloadInfo = e.downloadInfo.copy(fetchedBytes = episode.downloadInfo.fetchedBytes))
    }
    mediaProgressView.setSecondaryProgress(episode.downloadInfo.fetchedBytes.permille(episode.media.length).toInt)
  }

  override def onPreparingPlayback(episode: EpisodeBaseWithPlaybackInfo): Unit = {
    invalidateActionButtons()
  }

  override def onPlaybackStarted(episode: EpisodeBaseWithPlaybackInfo) {
    invalidateActionButtons()
  }

  override def onPlaybackPaused(episode: EpisodeBaseWithPlaybackInfo) {
    invalidateActionButtons()
  }

  override def onPlaybackStopped() {
    invalidateActionButtons()
  }

  override def onEpisodeCompleted(episode: EpisodeBaseWithPlaybackInfo) {
    invalidateActionButtons()
  }

  override def onAudioEffectsAvailable(available: Boolean): Unit = {
    playbackSpeedIndicator.foreach(_.makeInvisible(!available))
    volumeGainIndicator.foreach(_.makeInvisible(!available))
    if (available) {
      updateSpeedIndicator(playbackService.playbackSpeedMultiplier)
      updateVolumeGainIndicator(playbackService.volumeGain)
    }
  }

  override def onPlaybackSpeedChanged(playbackSpeed: Float): Unit =
    updateSpeedIndicator(playbackSpeed)

  override def onVolumeGainChanged(gain: Float): Unit =
    updateVolumeGainIndicator(gain)

  override def onSleepTimerModeChanged(mode: SleepTimerMode): Unit =
    sleepTimerIndicator.foreach(_.setMode(mode))

  private def updateSpeedIndicator(playbackSpeed: Float): Unit = {
    playbackSpeedIndicator.foreach { indicator =>
      indicator.setPlaybackSpeed(playbackSpeed)
    }
    updateFxIndicatorStatus(playbackSpeedIndicator)
  }

  private def updateVolumeGainIndicator(gain: Float): Unit = {
    volumeGainIndicator.foreach { indicator =>
      indicator.setGain(gain)
    }
    updateFxIndicatorStatus(volumeGainIndicator)
  }
  
  private def updateFxIndicatorStatus(optionalIndicator: Option[ActivatableIndicator]): Unit = optionalIndicator.foreach { indicator =>
    import mobi.upod.app.services.playback.player.AudioFxAvailability._

    log.trace("updating indicator status")
    playbackService.audioFxAvailability match {
      case Available =>
        indicator.show()
        indicator.setActive(true)
      case NotForCurrentPlayer | NotNow =>
        indicator.show()
        indicator.setActive(false)
      case _ =>
        indicator.makeInvisible()
    }    
  }

  private def updateFxIndicatorStatus(): Unit = {
    updateFxIndicatorStatus(playbackSpeedIndicator)
    updateFxIndicatorStatus(volumeGainIndicator)
  }

  private def onPlaybackSpeedIndicatorClicked(): Unit =
    onFxIndicatorClicked(R.string.playback_speed, PlaybackSpeedDialogFragment.show(getActivity, _))

  private def onVolumeGainIndicatorClicked(): Unit =
    onFxIndicatorClicked(R.string.volume_gain, AudioFxDialogFragment.show(getActivity, _))

  private def onFxIndicatorClicked(titleId: Int, editFx: EpisodeListItem => Unit): Unit = {
    import mobi.upod.app.services.playback.player.AudioFxAvailability._

    def showUnavailableMessage(msgId: Int, neutralButtonTextId: Int = R.string.close, yesAction: Option[Action] = None): Unit = {
      SimpleAlertDialogFragment.showFromActivity(
        getActivity,
        SimpleAlertDialogFragment.defaultTag,
        titleId,
        getActivity.getString(msgId),
        positiveButtonTextId = yesAction.map(_ => R.string.yes),
        neutralButtonTextId = Some(neutralButtonTextId),
        positiveAction = yesAction)
    }

    playbackService.audioFxAvailability match {
      case Available =>
        _episode.foreach(e => editFx(e))
      case NotNow =>
        showUnavailableMessage(R.string.audio_fx_not_now)
      case NotForCurrentPlayer =>
        showUnavailableMessage(R.string.audio_fx_not_for_current_player, R.string.no, Some(new ActivateSonicAudioPlayerAction))
      case _ => // ignore
    }
  }

  //
  // download listener
  //

  override def onDownloadProgress(episode: EpisodeBaseWithDownloadInfo, bytesPerSecond: Int, remainingMillis: Option[Long]) {
    if (_episode.exists(_.id == episode.id)) {
      onDownloadProgressChanged(episode)
    }
  }

  //
  // sync listener
  //

  override def onSyncFinished() {
    onPlaylistChanged()
  }

  //
  // episode listener
  //

  def onEpisodeCountChanged(): Unit = {
    if (_episode.isEmpty) {
      //showEmptyMessage()
    }
  }

  //
  // toggle media time format
  //

  def toggleMediaPositionFormat(): Unit = {
    val format = playbackPreferences.mediaTimeFormat.get match {
      case CurrentAndDuration =>
        RemainingAndDuration
      case RemainingAndDuration =>
        CurrentAndDuration
      case CurrentAndRemaining =>
        RemainingAndDuration
    }
    playbackPreferences.mediaTimeFormat := format
    updatePlaybackPosition()
  }

  def toggleMediaDurationFormat(): Unit = {
    val format = playbackPreferences.mediaTimeFormat.get match {
      case CurrentAndDuration =>
        CurrentAndRemaining
      case RemainingAndDuration =>
        CurrentAndRemaining
      case CurrentAndRemaining =>
        CurrentAndDuration
    }
    playbackPreferences.mediaTimeFormat := format
    updatePlaybackPosition()
  }

  //
  // support hooking into seek buttons
  //

  protected def onSeekAction(): Unit = {}

  trait SeekActionObserver extends Action {
    abstract override def onFired(context: Context) = {
      super.onFired(context)
      onSeekAction()
    }
  }
}
