package mobi.upod.app.gui.playback

import android.content.Context
import mobi.upod.android.app.action.Action
import mobi.upod.app.AppInjection
import mobi.upod.app.services.playback.PlaybackService
import mobi.upod.app.services.playback.player.SonicMediaPlayer
import mobi.upod.app.storage.{AudioPlayerType, PlaybackPreferences}

class ActivateSonicAudioPlayerAction extends Action with AppInjection {

  override def onFired(context: Context): Unit = if (SonicMediaPlayer.isAvailable) {
    val playbackService = inject[PlaybackService]
    val playing = playbackService.isPlaying
    playbackService.stop()
    inject[PlaybackPreferences].audioPlayerType := AudioPlayerType.Sonic
    playbackService.resume(Some(context))
    if (!playing) {
      playbackService.pause()
    }
  }
}
