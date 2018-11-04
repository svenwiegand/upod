package mobi.upod.app.services.playback.state

import mobi.upod.app.services.playback.PlaybackError

private[state] trait PlaybackErrorHandler {
  private var ignoreError = false

  /** Called for the current state when the MediaPlayer raises an error.
    * @return `true` if the error has been handled by the state an does not need to be propagated to the user `false` otherwise.
    */
  protected[state] def handlePlaybackError(error: PlaybackError): Boolean = ignoreError

  protected def ignoringPlaybackError[A](block: => A): A = {
    ignoreError = true
    try {
      block
    } finally {
      ignoreError = false
    }
  }

}
