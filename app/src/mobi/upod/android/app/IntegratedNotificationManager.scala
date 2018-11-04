package mobi.upod.android.app

import android.app
import android.app.{Notification, Service}
import android.content.Context
import mobi.upod.android.logging.Logging

trait IntegratedNotificationManager extends Logging { self: Service =>
  protected lazy val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE).asInstanceOf[app.NotificationManager]
  protected val notificationId = getClass.getName.hashCode

  def withNotification[A](notification: Notification, show: Boolean = true)(block: A): A = {
    if (show) {
      showNotification(notification)
    }
    try {
      block
    } finally {
      cancelNotification()
    }
  }

  def inForeground[A](notification: Notification, show: Boolean = true)(block: A): A = {
    if (show) {
      startForeground(notification)
    }
    try {
      block
    } finally if (show) {
      stopForeground()
    }
  }

  protected def showNotification(notification: Notification) {
    try notificationManager.notify(notificationId, notification) catch {
      case error: Throwable => log.error("failed to update notifciation", error)
    }
  }

  protected def updateNotification(notification: Notification) {
    showNotification(notification)
  }

  protected def cancelNotification() {
    notificationManager.cancel(notificationId)
  }

  protected def startForeground(notification: Notification) {
    startForeground(notificationId, notification)
  }

  protected def stopForeground() {
    stopForeground(true)
  }
}
