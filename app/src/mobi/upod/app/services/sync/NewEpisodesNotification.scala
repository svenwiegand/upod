package mobi.upod.app.services.sync

import android.app.{NotificationManager, PendingIntent}
import android.content.Context
import com.escalatesoft.subcut.inject.BindingModule
import mobi.upod.android.app.AppNotificationBuilder
import mobi.upod.app.R
import mobi.upod.app.gui.{MainActivity, MainNavigation}

object NewEpisodesNotification {
  private val NotificationId = R.drawable.ic_stat_new

  private def notificationManager(context: Context): NotificationManager =
    context.getSystemService(Context.NOTIFICATION_SERVICE).asInstanceOf[NotificationManager]

  def show(context: Context, count: Long)(implicit bindingModule: BindingModule): Unit = {
    val intent = MainActivity.intent(context, MainNavigation.newEpisodes)
    val notification = new AppNotificationBuilder(context).
      setSmallIcon(R.drawable.ic_stat_new).
      setContentTitle(context.getResources.getQuantityString(R.plurals.notify_new_episodes_title, count.toInt, count: java.lang.Long)).
      setContentText(context.getString(R.string.notify_new_episodes)).
      setContentInfo(count.toString).
      setContentIntent(PendingIntent.getActivity(context, NotificationId, intent, PendingIntent.FLAG_UPDATE_CURRENT)).
      build()
    notificationManager(context).notify(NotificationId, notification)
  }

  def cancel(context: Context): Unit =
    notificationManager(context).cancel(NotificationId)
}
