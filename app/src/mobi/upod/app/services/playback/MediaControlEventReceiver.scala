package mobi.upod.app.services.playback

import android.content.pm.PackageManager
import android.content.{BroadcastReceiver, ComponentName, Context, Intent}
import android.media.AudioManager
import mobi.upod.android.logging.Logging
import mobi.upod.android.util.ApiLevel
import mobi.upod.app.AppInjection
import mobi.upod.app.storage.PlaybackPreferences

class MediaControlEventReceiver extends BroadcastReceiver with MediaButtonProcessor with AppInjection with Logging {
  private lazy val playbackService = inject[PlaybackService]
  private lazy val playbackPreferences = inject[PlaybackPreferences]

  def onReceive(context: Context, intent: Intent): Unit = {
    intent.getAction match {
      case Intent.ACTION_MEDIA_BUTTON =>
        processMediaButtonEvent(intent)
      case AudioManager.ACTION_AUDIO_BECOMING_NOISY =>
        handleAudioBecomingNoisyEvent()
      case _ =>
    }
  }

  private def handleAudioBecomingNoisyEvent(): Unit = {
    log.info("received audio becoming noisy event")
    if (playbackPreferences.pauseWhenBecomingNoisy) {
      playbackService.pause()
    }
  }
}

object MediaControlEventReceiver {

  def componentName(context: Context) = new ComponentName(context, classOf[MediaControlEventReceiver])

  private def audioManager(context: Context): AudioManager =
    context.getSystemService(Context.AUDIO_SERVICE).asInstanceOf[AudioManager]

  def register(context: Context): Unit = {
    val name = componentName(context)
    context.getPackageManager.setComponentEnabledSetting(name, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
    if (ApiLevel < ApiLevel.Lollipop) {
      audioManager(context).registerMediaButtonEventReceiver(name)
    }
  }

  def unregister(context: Context): Unit = {
    val name = componentName(context)
    if (ApiLevel < ApiLevel.Lollipop) {
      audioManager(context).unregisterMediaButtonEventReceiver(name)
    }
    context.getPackageManager.setComponentEnabledSetting(name, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
  }
}