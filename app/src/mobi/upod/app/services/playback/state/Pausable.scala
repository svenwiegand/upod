package mobi.upod.app.services.playback.state

trait Pausable extends PlaybackState { this: Seekable =>

  def pause(joinRemotePause: Boolean = false): Unit =
    stateMachine.transitionToState(new Paused(episode, chapters, joinRemotePause))
}
