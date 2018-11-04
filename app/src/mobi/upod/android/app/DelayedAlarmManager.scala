package mobi.upod.android.app

import android.app.{PendingIntent, AlarmManager}
import android.content.Context
import org.joda.time.DateTime

class DelayedAlarmManager(context: Context) {
  private lazy val alarmManager = context.getSystemService(Context.ALARM_SERVICE).asInstanceOf[AlarmManager]

  def scheduleDelayedWakeup(delayMillis: Long, intent: PendingIntent): Unit =
    scheduleWakeup(System.currentTimeMillis + delayMillis, intent)

  def scheduleWakeup(systemMillis: Long, intent: PendingIntent): Unit =
    alarmManager.set(AlarmManager.RTC_WAKEUP, systemMillis, intent)

  def scheduleWakeup(time: DateTime, intent: PendingIntent): Unit =
    scheduleWakeup(time.getMillis, intent)
}
