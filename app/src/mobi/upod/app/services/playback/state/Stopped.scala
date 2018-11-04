package mobi.upod.app.services.playback.state

import com.escalatesoft.subcut.inject.BindingModule
import mobi.upod.app.services.download.DownloadService
import mobi.upod.app.services.playback.PlaybackError

private[playback] class Stopped(stopPlayer: Boolean = true)(implicit stateMachine: StateMachine, bindings: BindingModule)
  extends PlaybackState with Playable {

  override protected[state] def onEnterState() {
    super.onEnterState()

    try {
      if (stopPlayer) {
        player.stop()
      }
      player.reset()
      inject[DownloadService].stopBuffering()
    } catch {
      case ex: Throwable =>
        log.error("ignoring error during stop", ex)
    }
  }

  override protected[state] def handlePlaybackError(error: PlaybackError) = true
}
