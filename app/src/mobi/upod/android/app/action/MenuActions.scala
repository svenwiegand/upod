package mobi.upod.android.app.action

import android.view.{MenuItem, Menu}
import android.content.Context

trait MenuActions extends ActionController {

  protected def prepareMenu(menu: Menu)(implicit context: Context) {
    for (i <- 0 until menu.size) {
      prepareMenuItem(menu.getItem(i))
    }
  }

  protected def prepareMenuItem(item: MenuItem)(implicit context: Context) {
    actions.get(item.getItemId) foreach { action =>
      item.setVisible(action.isVisible(context))
      item.setEnabled(action.isEnabled(context))
    }
  }

  protected def onMenuItemSelected(item: MenuItem)(implicit context: Context): Boolean =
    fire(item.getItemId)
}
