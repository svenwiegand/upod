package mobi.upod.android.app.action

import ActionState._
import android.content.Context

trait ActionController {

  protected def createActions: Map[Int, Action]

  protected final lazy val actions: Map[Int, Action] = createActions

  protected def fire(actionId: Int)(implicit context: Context): Boolean = actions.get(actionId) match {
    case Some(action) =>
      if (action.state(context) == enabled) {
        action.fire(context)
      }
      true
    case None =>
      false
  }
}
