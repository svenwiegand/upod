package mobi.upod.android.widget

import mobi.upod.android.app.action.{Action, ActionController}
import mobi.upod.android.view.Helpers._
import android.view.ViewGroup

trait ActionButtons {
  protected var _actionButtons = Traversable[AbstractActionButton]()

  def actionButtons: Traversable[AbstractActionButton] = _actionButtons

  protected def actions: Map[Int, Action]

  def initActionButtons(rootView: ViewGroup): Unit = {

    def actionButton(actionId: Int, action: Action): Option[AbstractActionButton] = {
      rootView.optionalChildAs[AbstractActionButton](actionId).map(_.withAction(action))
    }

    _actionButtons = actions.iterator.toTraversable.map(e => actionButton(e._1, e._2)).filter(_.isDefined).map(_.get)
  }

  def invalidateActionButtons(): Unit =
    _actionButtons.foreach(_.invalidateAction())
}
