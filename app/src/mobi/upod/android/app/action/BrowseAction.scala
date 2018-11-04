package mobi.upod.android.app.action

import android.content.{Context, Intent}
import android.net.Uri

class BrowseAction(val url: String) extends Action {
  // verify valid Uri
  Uri.parse(url)

  def onFired(context: Context): Unit =
    context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)))
}
