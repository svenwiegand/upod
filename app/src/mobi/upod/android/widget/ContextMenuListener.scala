package mobi.upod.android.widget

import android.view.{Menu, MenuItem, View}
import android.widget.ImageView

trait ContextMenuListener {

  def onCreateMenu(menu: Menu, view: View): Unit

  def onPrepareMenu(menu: Menu): Unit

  def onPreparePrimaryAction(item: MenuItem, primaryActionButton: ImageView): Unit = ()

  def onContextItemSelected(item: MenuItem): Boolean
}
