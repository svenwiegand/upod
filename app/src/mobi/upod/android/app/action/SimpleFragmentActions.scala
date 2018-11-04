package mobi.upod.android.app.action

import android.view.{MenuInflater, Menu}
import android.os.Bundle

trait SimpleFragmentActions extends FragmentActions {

  protected def optionsMenuResourceId: Int

  protected def hasOptionsMenu = true

  override def onActivityCreated(savedInstanceState: Bundle): Unit = {
    super.onActivityCreated(savedInstanceState)
    setHasOptionsMenu(hasOptionsMenu)
  }

  override def onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
    super.onCreateOptionsMenu(menu, inflater)
    inflater.inflate(optionsMenuResourceId, menu)
  }

  protected def invalidateOptionsMenu(): Unit = {
    Option(getActivity).foreach(_.invalidateOptionsMenu())
  }
}
