package mobi.upod.app.services.sync

import android.app.PendingIntent
import android.content.{Context, Intent}
import mobi.upod.android.app.AppException
import mobi.upod.android.app.action.GooglePlayAction

class AppVersionExpiredException(context: Context) extends AppException(
  activityIntent = Some(GooglePlayAction.defaultIntent(context.getPackageName))
)
