package mobi.upod.android.app.action

import android.view.{ActionMode, Menu, MenuItem}
import android.widget.AbsListView.MultiChoiceModeListener
import android.content.Context

trait ContextualActions extends MultiChoiceModeListener with MenuActions with ImplicitContext {
  protected def contextualMenuResourceId: Int
  private var actionMode: Option[ActionMode] = None

  def onItemCheckedStateChanged(mode: ActionMode, position: Int, id: Long, checked: Boolean) {
    mode.invalidate()
  }

  def onCreateActionMode(mode: ActionMode, menu: Menu): Boolean = {
    actionMode = Some(mode)
    mode.getMenuInflater.inflate(contextualMenuResourceId, menu)
    true
  }

  def onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean = {
    prepareMenu(menu)
    true
  }

  def onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean =
    onMenuItemSelected(item)

  def onDestroyActionMode(mode: ActionMode): Unit = {
    actionMode = None
    // Here you can make any necessary updates to the activity when
    // the CAB is removed. By default, selected items are deselected/unchecked.
  }

  def invalidateActionMode(): Unit =
    actionMode.foreach(_.invalidate())

  def destroyActionMode(): Unit =
    actionMode.foreach(_.finish())
}
