package mobi.upod.app.services.playback.state

private[playback] trait Stoppable extends PlaybackState {

  def stop() {
    stateMachine.transitionToState(new Stopped())
  }
}
