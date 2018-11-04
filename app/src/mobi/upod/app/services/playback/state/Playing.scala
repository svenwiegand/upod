package mobi.upod.app.services.playback.state

import java.util.Timer

import com.escalatesoft.subcut.inject.BindingModule
import mobi.upod.android.os.PowerManager
import mobi.upod.app.data.EpisodeListItem
import mobi.upod.app.services.playback.player.MediaPlayer
import mobi.upod.app.services.playback.player.MediaPlayer.OnCompletionListener
import mobi.upod.app.services.sync.SyncService
import mobi.upod.media.MediaChapterTable
import mobi.upod.util.Duration._
import mobi.upod.util.TimerTask

import scala.util.Try

private[playback] class Playing(
    protected val initialEpisode: EpisodeListItem,
    val chapters: MediaChapterTable,
    joinRemotePlayback: Boolean = false)(
    implicit
    stateMachine: StateMachine,
    bindings: BindingModule)
  extends PlaybackState
  with Pausable
  with Seekable
  with Stoppable
  with StateWithPlaybackPosition
  with AudioFocusable
  with OnCompletionListener {

  private val ProgressUpdateInterval = 1.second

  private val wakeLock = inject[PowerManager].partialWakeLock()
  private val updateProgressTimer = new Timer("PlaybackProgressUpdateTimer")

  override protected[state] def onEnterState() {
    super.onEnterState()
    player.setOnCompletionListener(this)
    if (!joinRemotePlayback) {
      player.start()
    }
    updateProgressTimer.schedule(TimerTask(updateProgressIfActive()), ProgressUpdateInterval, ProgressUpdateInterval)
  }

  override protected[state] def onExitState() {
    super.onExitState()
    Try(player.setOnCompletionListener(null))
    updateProgressTimer.cancel()
    log.debug("onExitState")
    updateProgress(true)
    inject[SyncService].pushSyncRequired()
    Try(wakeLock.release()).recover { case err =>
      log.crashLogError(err.getMessage, err)
    }
  }

  private def updateProgressIfActive(): Unit = if (isActive && player.isPlayerSet) {
    try updateProgress() catch {
      case ex: Throwable =>
        log.error("failed to update progress", ex)
    }
  }

  def fadeOut(): Unit = {
    if ((player.getDuration - player.getCurrentPosition) > (FadingOut.FadeTime + 5000))
      transitionToState(new FadingOut(initialEpisode, chapters))
    else
      pause()
  }

  //
  // listeners
  //

  override def onAudioFocusLostTransient() {
    transitionToState(new FocusLostTransiently(episode, chapters))
  }

  def onCompletion(mp: MediaPlayer) {
    transitionToState(new Completed(episode, chapters))
  }
}
