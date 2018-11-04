package mobi.upod.app.services.playback.state

private[playback] trait AudioFocusable extends PlaybackState {

  def onAudioFocusGranted() {}

  def onAudioFocusLostTransient() {}
}
