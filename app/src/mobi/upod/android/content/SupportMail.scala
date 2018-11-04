package mobi.upod.android.content

import android.content.{Intent, Context}
import java.io.File
import android.content.pm.PackageManager
import java.util
import android.net.Uri
import mobi.upod.app.R
import scala.collection.JavaConverters._

object SupportMail {

  def supportEmailAddress(context: Context): String = {
    val metaData = context.getPackageManager.getApplicationInfo(context.getPackageName, PackageManager.GET_META_DATA).metaData
    metaData.getString("supportEmail")
  }

  def send(context: Context, subject: String, body: String, attachements: Iterable[Uri] = Iterable()): Unit = {
    val intent = new Intent(Intent.ACTION_SEND_MULTIPLE)
    intent.putExtra(Intent.EXTRA_EMAIL, Array(supportEmailAddress(context)))
    intent.putExtra(Intent.EXTRA_SUBJECT, subject)
    intent.putExtra(Intent.EXTRA_TEXT, body)

    intent.setType("text/plain")
    val fileUris = new util.ArrayList(attachements.asJavaCollection)
    intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, fileUris)
    context.startActivity(Intent.createChooser(intent, context.getString(R.string.send_email)))
  }
}
