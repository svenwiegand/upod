package mobi.upod.android.app.action

import android.support.v7.app.ActionBarActivity
import android.view.{Menu, MenuItem}

trait ActivityActions extends ActionBarActivity with MenuActions with ImplicitActivityContext {
  protected val optionsMenuResourceId: Int

  override def onCreateOptionsMenu(menu: Menu) = {
    getMenuInflater.inflate(optionsMenuResourceId, menu)
    super.onCreateOptionsMenu(menu)
    true
  }

  override def onPrepareOptionsMenu(menu: Menu) = {
    super.onPrepareOptionsMenu(menu)
    prepareMenu(menu)
    true
  }

  override def onOptionsItemSelected(item: MenuItem): Boolean =
    onMenuItemSelected(item) || super.onOptionsItemSelected(item)
}
