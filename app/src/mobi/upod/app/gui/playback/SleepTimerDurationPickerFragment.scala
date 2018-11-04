package mobi.upod.app.gui.playback

import mobi.upod.app.AppInjection
import mobi.upod.app.services.playback.{SleepTimerMode, PlaybackService}
import mobi.upod.app.storage.PlaybackPreferences
import mobi.upod.timedurationpicker.{TimeDurationPicker, TimeDurationPickerDialogFragment}

final class SleepTimerDurationPickerFragment extends TimeDurationPickerDialogFragment with AppInjection {
  private lazy val latestSleepTimerDuration = inject[PlaybackPreferences].sleepTimerDuration

  override def getInitialDuration: Long =
    latestSleepTimerDuration.get

  override def onDurationSet(timeDurationPicker: TimeDurationPicker, l: Long): Unit = {
    inject[PlaybackService].startSleepTimer(SleepTimerMode.Timer(l))
    latestSleepTimerDuration := l
  }
}