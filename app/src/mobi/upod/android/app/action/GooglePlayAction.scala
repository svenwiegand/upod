package mobi.upod.android.app.action

import android.content.{Intent, Context}
import android.net.Uri
import mobi.upod.android.content.GooglePlay

class GooglePlayAction(packageName: String) extends Action {
  
  def onFired(context: Context): Unit = {
    try openUri(context, GooglePlayAction.defaultIntent(packageName)) catch {
      case ex: android.content.ActivityNotFoundException =>
        openUri(context, GooglePlayAction.fallbackIntent(packageName))
    }
  }
  
  private def openUri(context: Context, intent: Intent): Unit =
    context.startActivity(intent)
}

object GooglePlayAction {

  def defaultIntent(packageName: String): Intent =
    intent(GooglePlay.intentUrl(packageName))

  def fallbackIntent(packageName: String): Intent =
    intent(GooglePlay.webUrl(packageName))

  private def intent(uri: String): Intent =
    new Intent(Intent.ACTION_VIEW, Uri.parse(uri))
}
