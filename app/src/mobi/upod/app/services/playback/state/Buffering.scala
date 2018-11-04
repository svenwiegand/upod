package mobi.upod.app.services.playback.state

private[playback] trait Buffering
  extends PlaybackState
  with StateWithEpisode
  with Stoppable
