package mobi.upod.app.services.playback.state

import com.escalatesoft.subcut.inject.BindingModule
import mobi.upod.app.data.EpisodeListItem

private[state] final class RemoteBuffering(val episode: EpisodeListItem)(implicit stateMachine: StateMachine, bindings: BindingModule)
  extends Buffering
  with TransitionToSeekable {

  def onRemotePlay(): Unit =
    transitionTo(new Playing(episode, _, true))

  def onRemotePause(): Unit =
    transitionTo(new Paused(episode, _, true))
}
