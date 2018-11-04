package mobi.upod.app.services.playback.state

private[playback] trait Resumable extends StateWithEpisode { this: Seekable =>

  def resume(): Unit =
    stateMachine.transitionToState(new Playing(episode, chapters))

  def remoteResume(): Unit =
    stateMachine.transitionToState(new Playing(episode, chapters, true))
}
