package mobi.upod.app.services.subscription

import java.io.File

import android.content.{Intent, Context}
import android.net.Uri
import com.escalatesoft.subcut.inject.{BindingModule, Injectable}
import mobi.upod.android.app.action.{ActionWaitDialog, AsyncAction}
import mobi.upod.app.R
import mobi.upod.app.storage.PodcastDao

class ExportSubscriptionsAction(implicit val bindingModule: BindingModule)
  extends AsyncAction[Unit, File]
  with ActionWaitDialog
  with Injectable {

  override protected def getData(context: Context): Unit = ()

  override protected def processData(context: Context, data: Unit): File =
    writeOpml(context)

  override protected def postProcessData(context: Context, result: File): Unit =
    sendOpml(context, result)

  private def writeOpml(context: Context): File = {
    val file = new File(context.getCacheDir, "subscriptions.opml")
    val dao = inject[PodcastDao]
    dao.findSubscriptionListItems doAndClose { subscriptions =>
      OpmlWriter.write(file, subscriptions.map(s => s.title -> s.url))
    }
    file.setReadable(true, false)
    file
  }

  private def sendOpml(context: Context, file: File): Unit = {
    val intent = new Intent(Intent.ACTION_SEND)
    intent.setType("text/x-opml")
    intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file))
    context.startActivity(Intent.createChooser(intent, context.getString(R.string.action_export_subscriptions)))
  }
}
