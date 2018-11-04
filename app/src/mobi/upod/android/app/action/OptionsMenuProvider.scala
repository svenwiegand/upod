package mobi.upod.android.app.action

import android.view.{MenuItem, Menu}

trait OptionsMenuProvider {

  protected def onPrepareOptionsMenu(menu: Menu)

  protected def onOptionsItemSelected(item: MenuItem): Boolean
}
