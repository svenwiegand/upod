package mobi.upod.android.app.action

import android.view.{Menu, MenuInflater, MenuItem, View}
import mobi.upod.android.widget.ContextMenuListener

trait ContextMenuActions extends MenuActions with ContextMenuListener with ImplicitContext {

  protected def contextMenuResourceId: Int

  def onCreateMenu(menu: Menu, view: View): Unit =
    new MenuInflater(view.getContext).inflate(contextMenuResourceId, menu)

  override def onPrepareMenu(menu: Menu): Unit =
    prepareMenu(menu)

  def onContextItemSelected(item: MenuItem): Boolean =
    onMenuItemSelected(item)
}
