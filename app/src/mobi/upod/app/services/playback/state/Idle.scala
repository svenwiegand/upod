package mobi.upod.app.services.playback.state

import com.escalatesoft.subcut.inject.BindingModule

private[playback] final class Idle(implicit stateMachine: StateMachine, bindings: BindingModule)
  extends PlaybackState with Playable {

  override protected[state] def onEnterState() {
    super.onEnterState()
    player.reset()
  }
}
