package mobi.upod.android.app.action

import android.app.Fragment
import android.view.{MenuInflater, Menu, MenuItem}

trait FragmentActions extends Fragment with MenuActions with ImplicitFragmentContext {

  override def onPrepareOptionsMenu(menu: Menu) {
    super.onPrepareOptionsMenu(menu)
    prepareMenu(menu)
  }

  override def onOptionsItemSelected(item: MenuItem): Boolean =
    onMenuItemSelected(item) || super.onOptionsItemSelected(item)
}
