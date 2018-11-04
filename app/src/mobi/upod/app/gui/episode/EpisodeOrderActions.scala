package mobi.upod.app.gui.episode

import android.content.Context
import mobi.upod.android.app.action.ActionState.ActionState
import mobi.upod.android.app.action.{Action, ActionState}
import mobi.upod.app.R
import mobi.upod.app.data.EpisodeListItem

trait EpisodeOrderActions extends EpisodeListItemViewHolder {
  private var _itemPosition: Option[Int] = None

  override protected def createActions: Map[Int, Action] = Map(
    R.id.action_move_to_top -> MoveToTopAction,
    R.id.action_move_to_bottom -> MoveToBottomAction
  )

  override def setItem(position: Int, item: EpisodeListItem): Unit = {
    _itemPosition = Some(position)
    super.setItem(position, item)
  }

  private class MoveAction(canMove: (EpisodeOrderControl, Int) => Boolean, move: (EpisodeOrderControl, Int) => Unit) extends Action {

    override def state(context: Context): ActionState = {
      if (_itemPosition.exists(pos => config.episodeOrderControl.exists(ctrl => canMove(ctrl, pos))))
        ActionState.enabled
      else
        ActionState.gone
    }

    override def onFired(context: Context): Unit = config.episodeOrderControl.foreach { orderControl =>
      _itemPosition.foreach(pos => move(orderControl, pos))
    }
  }

  private object MoveToTopAction extends MoveAction(_.canMoveToTop(_), _.moveToTop(_))
  private object MoveToBottomAction extends MoveAction(_.canMoveToBottom(_), _.moveToBottom(_))
}
