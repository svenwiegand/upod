package mobi.upod.android.app

import android.content.Context
import android.support.v7.app.NotificationCompat
import android.support.v7.appcompat.R

import scala.util.Try

class AppNotificationBuilder(context: Context) extends NotificationCompat.Builder(context) {
  applyPrimaryColor()

  private def applyPrimaryColor(): Unit =
    Try(context.getApplicationContext.getResources.getColor(R.attr.colorPrimary))
}
