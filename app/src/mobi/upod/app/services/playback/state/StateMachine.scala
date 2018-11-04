package mobi.upod.app.services.playback.state

import java.net.URL

import android.content.Context
import com.escalatesoft.subcut.inject.Injectable
import mobi.upod.android.logging.Logging
import mobi.upod.android.os.AsyncExecution
import mobi.upod.app.data.{EpisodeBaseWithPlaybackInfo, EpisodeListItem}
import mobi.upod.app.services.playback.player.MediaPlayer._
import mobi.upod.app.services.playback.player.{MediaPlayer, SwitchableMediaPlayer}
import mobi.upod.app.services.playback.{PlaybackError, PlaybackListener, RemotePlaybackState}
import mobi.upod.app.storage.{EpisodeDao, PodcastDao, StoragePreferences}

import scala.reflect.ClassTag

trait StateMachine
  extends Injectable
  with OnErrorListener
  with OnAudioFxListener
  with OnRemotePlayerStateChangedListener
  with OnRemoteEpisodeChangedListener
  with OnPlayerDisconnectedListener
  with LocalBufferingListener
  with AsyncExecution
  with Logging {
  self: Context =>

  private implicit val stateMachine: StateMachine = this
  private var _state: PlaybackState = new Idle
  private var _processingError: Boolean = false

  protected val podcastDao = inject[PodcastDao]
  protected val episodeDao = inject[EpisodeDao]

  protected[playback] def player: SwitchableMediaPlayer

  protected def state: PlaybackState = _state

  private[state] def transitionToState(state: PlaybackState) {
    log.crashLogInfo(s"transitioning from playback state ${_state} to $state")
    val oldState = _state
    oldState.onExitState()
    _state = state
    _state.onEnterState()
    onStateChanged(oldState, _state)
  }

  protected def onStateChanged(oldState: PlaybackState, newState: PlaybackState)

  protected def reset() {
    ifNotIs[Idle] {
      transitionToState(new Idle)
    }
  }

  protected def forceStop(stopPlayer: Boolean = true): Unit = {
    try transitionToState(new Stopped(stopPlayer)) catch {
      case t: Throwable => log.crashLogError("ignoring failure while stopping player", t)
    }
  }

  protected def is[A <: PlaybackState](implicit stateTag: ClassTag[A]): Boolean = _state match {
    case _: A => true
    case _ => false
  }

  protected def ifIs[A <: PlaybackState](block: A => Unit)(implicit stateTag: ClassTag[A]) {
    _state match {
      case s: A => block(s)
      case _ =>
    }
  }

  protected def ifNotIs[A <: PlaybackState](block: => Unit)(implicit stateTag: ClassTag[A]) {
    _state match {
      case s: A =>
      case _ => block
    }
  }

  protected def stateAs[A <: PlaybackState](implicit stateTag: ClassTag[A]): Option[A] = _state match {
    case s: A => Some(s)
    case _ => None
  }

  protected[state] def fire(event: PlaybackListener => Unit)

  //
  // remote player state change listener
  //

  def joinRemoteSession(mediaUrl: URL, state: RemotePlaybackState.RemotePlaybackState): Unit

  override def onRemotePlaying(mediaPlayer: MediaPlayer, mediaUrl: URL): Unit = state match {
    case s: Playable with StateWithEpisode => s.joinRemoteSession(s.episode, RemotePlaybackState.Playing)
    case s: RemoteBuffering => s.onRemotePlay()
    case s: Paused => s.remoteResume()
    case _ => joinRemoteSession(mediaUrl, RemotePlaybackState.Playing)
  }

  override def onRemotePaused(mediaPlayer: MediaPlayer, mediaUrl: URL): Unit = state match {
    case s: Pausable => s.pause(true)
    case s: RemoteBuffering => s.onRemotePause()
    case _ => joinRemoteSession(mediaUrl, RemotePlaybackState.Paused)
  }

  override def onRemoteBuffering(mediaPlayer: MediaPlayer, mediaUrl: URL): Unit = state match {
    case state: StateWithEpisode if is[Playing] || is[Paused] || is[Completed] =>
      transitionToState(new RemoteBuffering(state.episode))
    case _ =>
      joinRemoteSession(mediaUrl, RemotePlaybackState.Buffering)
  }

  override def onRemoteStopped(mediaPlayer: MediaPlayer): Unit = {
    forceStop()
  }

  //
  // error listener
  //

  def onError(mp: MediaPlayer, error: PlaybackError): Boolean = {
    if (_state.handlePlaybackError(error))
      log.crashLogWarn(s"state ${_state} suppressed playback error $error")
    else
      onError(error)
    true
  }

  def onError(ex: Throwable, error: PlaybackError): Unit = {
    log.crashLogError(s"unhandled exception in state ${_state}", ex)
    onError(error)
  }

  private def onError(error: PlaybackError): Unit = if (!_processingError) {
    _processingError = true
    val episode = stateAs[StateWithEpisode].map(_.episode)
    reset()

    val storageProvider = inject[StoragePreferences].storageProvider
    val specificError = error.reason match {
      case PlaybackError.Unknown => episode match {
        case Some(e) =>
          if (!storageProvider.readable) {
            PlaybackError(PlaybackError.StorageNotAvailable, error.what, error.extra)
          } else {
            val file = e.mediaFile(storageProvider)
            if (file.exists)
              PlaybackError(PlaybackError.UnsupportedFormat, error.what, error.extra)
            else if (e.downloadInfo.fetchedBytes > 0) {
              episodeDao.inTransaction(episodeDao.resetDownloadInfo(e.id))
              PlaybackError(PlaybackError.FileDoesNotExist, error.what, error.extra)
            } else
              error
          }
        case _ =>
          error
      }
      case _ =>
        error
    }
    val msg = s"unhandled playback error $specificError (derived from $error) in state ${_state} for file ${episode.map(e => storageProvider.whenReadable(e.mediaFile))}"
    log.crashLogError(msg)
    onPlaybackError(episode, specificError)
    _processingError = false
  }

  protected def onPlaybackError(episode: Option[EpisodeListItem], error: PlaybackError): Unit

  override def onPlayerDisconnected(mediaPlayer: MediaPlayer): Unit = {
    log.info("player disconnected")
    forceStop(false)
    player.release()
  }

  //
  // speed adjustment
  //

  def setAudioEffects(episode: EpisodeBaseWithPlaybackInfo): Unit = {
    lazy val podcastEffects = podcastDao.getAudioEffects(episode.podcastInfo.id)

    val speed = episode.playbackInfo.playbackSpeed match {
      case Some(playbackSpeed) => playbackSpeed
      case None => podcastEffects.flatMap(_.playbackSpeed).getOrElse(1f)
    }
    player.setPlaybackSpeedMultiplier(speed)

    val gain = episode.playbackInfo.volumeGain match {
      case Some(volumeGain) => volumeGain
      case None => podcastEffects.flatMap(_.volumeGain).getOrElse(0f)
    }
    player.setVolumeGain(gain)
  }

  def onAudioFxAvailable(available: Boolean): Unit = {
    fire(_.onAudioEffectsAvailable(available))
    if (available && (is[Playing] || is[Paused])) {
      val state = _state.asInstanceOf[StateWithEpisode]
      setAudioEffects(state.episode)
    }
  }

  def onPlaybackSpeedChange(multiplier: Float): Unit =
    fire(_.onPlaybackSpeedChanged(multiplier))

  def onVolumeGainChange(gain: Float): Unit =
    fire(_.onVolumeGainChanged(gain))
}
