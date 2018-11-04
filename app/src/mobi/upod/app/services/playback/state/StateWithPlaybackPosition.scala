package mobi.upod.app.services.playback.state

private[playback] trait StateWithPlaybackPosition extends PlaybackState {

  def playbackPosition = player.getCurrentPosition
}
