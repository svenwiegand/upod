package mobi.upod.android.app.action

import android.app.Activity
import android.content.Context
import mobi.upod.android.app.ConfirmationDialog
import mobi.upod.android.app.action.ActionState.ActionState

class ConfirmedAction(titleId: Int, message: => CharSequence, confirmedAction: => Action) extends Action {
  private lazy val action = confirmedAction

  def this(titleId: Int, message: => CharSequence, fragment: ConfirmedActionProviderFragment, tag: String) =
    this(titleId, message, new ConfirmedAction.ProviderFragmentAction(fragment.getId, tag))

  override def state(context: Context): ActionState = 
    action.state(context)

  override def onFired(context: Context): Unit = {
    context match {
      case activity: Activity => showConfirmationDialog(activity)
      case _ =>
    }
  }

  private def showConfirmationDialog(activity: Activity): Unit =
    ConfirmationDialog.show(activity, titleId, message, action)
}

private object ConfirmedAction {

  private class ProviderFragmentAction(val fragmentId: Int, val tag: String) extends Action {

    private def action(context: Context): Option[Action] = context match {
      case activity: Activity => activity.getFragmentManager.findFragmentById(fragmentId) match {
        case fragment: ConfirmedActionProviderFragment => Some(fragment.confirmedAction(tag))
        case _ => None
      }
      case _ => None
    }

    override def state(context: Context): ActionState =
      action(context).map(_.state(context)).getOrElse(ActionState.disabled)

    override def onFired(context: Context): Unit =
      action(context).foreach(_.fire(context))
  }
}