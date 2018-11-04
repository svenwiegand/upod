package mobi.upod.app.services.playback.state

import com.escalatesoft.subcut.inject.{BindingModule, Injectable}
import mobi.upod.android.logging.Logging
import mobi.upod.app.services.playback.PlaybackListener
import mobi.upod.app.services.playback.player.{SwitchableMediaPlayer, MediaPlayer}


private[playback] class PlaybackState(implicit protected val stateMachine: StateMachine, val bindingModule: BindingModule)
  extends Injectable
  with PlaybackErrorHandler
  with Logging {

  private var active: Boolean = false

  def isActive = active

  protected def player: SwitchableMediaPlayer = stateMachine.player

  protected def transitionToState(state: PlaybackState) {
    stateMachine.transitionToState(state)
  }

  protected[state] def onEnterState() {
    active = true
  }

  protected[state] def onExitState() {
    active = false
  }

  protected def fire(event: PlaybackListener => Unit) {
    stateMachine.fire(event)
  }

  override def toString = getClass.getSimpleName
}
