package mobi.upod.app.services.subscription

import com.google.api.client.http.HttpResponseException
import java.io.FileNotFoundException
import mobi.upod.android.app.AppException
import android.content.Context
import android.net.Uri
import mobi.upod.android.content.SupportMail
import org.xml.sax.SAXParseException

abstract class OpmlException(opml: Uri, cause: Throwable, message: Option[String] = None)
  extends AppException(message, Some(cause)) {

  def sendSupportMail(context: Context): Unit = {
    SupportMail.send(context, s"uPod OPML import failed", s"OPML import failed with the following error:\n\n$this", supportMailFileUris)
  }

  protected def supportMailFileUris: Iterable[Uri] =
    Iterable(opml)
}

class OpmlFileNotFoundException(opml: Uri, cause: FileNotFoundException) extends OpmlException(opml, cause) {
  override protected def supportMailFileUris = Iterable()
}

class OpmlParseException(opml: Uri, cause: SAXParseException) extends OpmlException(opml, cause, Option(cause.getLocalizedMessage)) {

  override def errorTitle(implicit context: Context) =
    resourceString(errorTitleKey, context.getString(_, Option(cause.getLocalizedMessage).getOrElse("")))
}

class GenericOpmlAppException(opml: Uri, cause: AppException) extends OpmlException(opml, cause) {

  override def errorTitle(implicit context: Context) = cause.errorTitle
}

class GenericOpmlException(opml: Uri, cause: Throwable) extends OpmlException(opml, cause, Option(cause.getMessage)) {

  override def errorTitle(implicit context: Context) =
    resourceString(errorTitleKey, context.getString(_, Option(cause.getLocalizedMessage).getOrElse("")))
}

object OpmlException {
  def apply(opml: Uri, cause: Throwable): OpmlException = cause match {
    case ex: FileNotFoundException =>
      new OpmlFileNotFoundException(opml, ex)
    case ex: SAXParseException =>
      new OpmlParseException(opml, ex)
    case ex: AppException =>
      new GenericOpmlAppException(opml, ex)
    case ex =>
      new GenericOpmlException(opml, ex)
  }
}