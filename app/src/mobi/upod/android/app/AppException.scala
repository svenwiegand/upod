package mobi.upod.android.app

import android.content.res.Resources
import android.content.{Context, Intent}

class AppException(message: Option[String] = None, cause: Option[Throwable] = None, activityIntent: Option[Intent] = None)
  extends Exception(message.getOrElse(cause map { _.getMessage } getOrElse("")), cause.getOrElse(null)) {
  private val exceptionSuffix = "Exception"

  lazy val errorCode: String = {
    val className = getClass.getSimpleName
    val errorName = (if (className.endsWith(exceptionSuffix))
      className.substring(0, className.length - exceptionSuffix.length)
    else
      className) + "Error"
    errorName.flatMap(char => if (char.isUpper) s"_${char.toLower}" else char.toString).tail
  }

  private def resourceTextId(key: String)(implicit context: Context): Int =
    context.getResources.getIdentifier(key, "string", context.getPackageName)

  protected def resourceString(key: String, stringFromId: Int => String)(implicit context: Context): String = try {
    stringFromId(resourceTextId(key))
  } catch {
    case ex: Resources.NotFoundException => key
  }

  protected def errorTitleKey: String = errorCode

  def errorTitle(implicit context: Context): String =
    resourceString(errorTitleKey, context.getString)

  protected def errorTextKey: String = s"${errorCode}_details"

  def errorText(implicit context: Context): String =
    resourceString(errorTextKey, context.getString)

  def intent: Option[Intent] = activityIntent
}
