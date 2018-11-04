package mobi.upod.app.services.playback.state

import com.escalatesoft.subcut.inject.BindingModule
import mobi.upod.app.data.EpisodeListItem
import mobi.upod.media.MediaChapterTable

private[playback] class FocusLostTransiently(
    val initialEpisode: EpisodeListItem,
    val chapters: MediaChapterTable)(
    implicit
    stateMachine: StateMachine,
    bindings: BindingModule)
  extends PlaybackState
  with Pausable
  with Seekable
  with Stoppable
  with AudioFocusable
  with StateWithPlaybackPosition {

  override protected[state] def onEnterState(): Unit = {
    super.onEnterState()
    player.pause()
  }

  //
  // audio focus
  //

  override def onAudioFocusGranted() {
    transitionToState(new Playing(episode, chapters))
  }
}
