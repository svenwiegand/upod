package mobi.upod.app.gui.playback

import android.content.{ActivityNotFoundException, Intent, Context}
import mobi.upod.android.app.action.Action
import android.net.Uri
import mobi.upod.android.logging.Logging

class PlayWithExternalPlayerAction(dataUri: String, mimeType: String) extends Action with Logging {

  override def onFired(context: Context): Unit = {
    val intent = new Intent(Intent.ACTION_VIEW)
    intent.setDataAndType(Uri.parse(dataUri), mimeType)
    try context.startActivity(intent) catch {
      case ex: ActivityNotFoundException => log.warn("no activity found to playback media file", ex)
    }
  }
}
