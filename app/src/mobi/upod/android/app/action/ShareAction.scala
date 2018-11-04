package mobi.upod.android.app.action

import mobi.upod.android.app.action.ShareAction.SharedData
import android.content.{Intent, Context}
import ActionState._
import android.net.{ParseException, Uri}

class ShareAction(dialogTitleId: Int, sharedData: Context => Option[SharedData]) extends Action {

  override def state(context: Context): ActionState =
    if (sharedData(context).isDefined) enabled else gone

  def onFired(context: Context): Unit = sharedData(context) foreach { data =>
    context.startActivity(Intent.createChooser(data.intent, context.getString(dialogTitleId)))
  }
}

object ShareAction {
  case class SharedData(content: String, subject: String) {

    def intent: Intent = {
      val intent = new Intent(Intent.ACTION_SEND)
      intent.setType("text/plain")
      intent.putExtra(Intent.EXTRA_SUBJECT, subject)
      intent.putExtra(Intent.EXTRA_TEXT, content)
    }
  }
}