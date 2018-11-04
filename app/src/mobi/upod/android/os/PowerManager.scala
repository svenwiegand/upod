package mobi.upod.android.os

import android.content.Context
import android.os.{PowerManager => AndroidPowerManager}

class PowerManager(context: Context) {
  type WakeLock = android.os.PowerManager#WakeLock

  lazy val osPowerManager = context.getSystemService(Context.POWER_SERVICE).asInstanceOf[AndroidPowerManager]

  private def acquire(flags: Int): WakeLock = {
    val wakeLock = osPowerManager.newWakeLock(flags, "power manager")
    wakeLock.acquire()
    wakeLock
  }

  def partialWakeLock(): WakeLock =
    acquire(AndroidPowerManager.PARTIAL_WAKE_LOCK)

  def partiallyWaked[A](block: => A): A = {
    val wakeLock = partialWakeLock()
    try {
      block
    } finally {
      wakeLock.release()
    }
  }
}
