package mobi.upod.app.services.playback.state

import com.escalatesoft.subcut.inject.BindingModule
import mobi.upod.app.data.EpisodeListItem
import mobi.upod.app.services.playback.player.MediaPlayer
import mobi.upod.app.services.playback.player.MediaPlayer.OnAsyncPositionUpdateListener
import mobi.upod.media.MediaChapterTable

private[playback] class Paused(
    protected val initialEpisode: EpisodeListItem,
    val chapters: MediaChapterTable,
    joinRemotePause: Boolean)(
    implicit
    stateMachine: StateMachine,
    bindings: BindingModule)
  extends PlaybackState
  with Seekable
  with Resumable
  with Stoppable
  with StateWithPlaybackPosition
  with OnAsyncPositionUpdateListener {

  override protected[state] def onEnterState(): Unit = {
    super.onEnterState()
    player.setOnAsyncPositionUpdateListener(this)
    if (!joinRemotePause) {
      player.pause()
    }
  }

  override protected[state] def onExitState(): Unit = {
    player.setOnAsyncPositionUpdateListener(null)
    super.onExitState()
  }

  override def onPositionUpdated(mediaPlayer: MediaPlayer, position: Int): Unit = {
    log.debug(s"onPositionUpdated to $position")
    updateProgress()
  }
}
