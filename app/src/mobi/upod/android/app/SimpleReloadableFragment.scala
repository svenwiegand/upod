package mobi.upod.android.app

import android.app.Fragment
import android.app.LoaderManager.LoaderCallbacks
import android.os.Bundle
import mobi.upod.android.content.Reloadable

trait SimpleReloadableFragment[A]
  extends Fragment
  with LoaderCallbacks[A]
  with Reloadable {

  private var reloadRequired = false

  protected def loaderId = 0

  protected def loaderBundle: Option[Bundle] = None

  protected def reloadOnCreate: Boolean = true

  override def onActivityCreated(savedInstanceState: Bundle) {
    super.onActivityCreated(savedInstanceState)
    if (reloadOnCreate)
      reload()
  }

  override def onStart() {
    super.onStart()
    reloadIfRequired()
  }

  private def reloadIfRequired(): Unit = {
    if (reloadRequired) {
      onAutoReload()
    }
  }

  def onAutoReload(): Unit = {
    reload()
  }

  def reload(): Unit = {
    reloadRequired = false
    try {
      getLoaderManager.restartLoader(loaderId, loaderBundle.getOrElse(null), this)
    } catch {
      case ex: IllegalStateException =>
        // ignore: Already creating a loader, so everything is fine
    }
  }

  def requestReload(): Unit = {
    if (isVisible)
      onAutoReload()
    else
      reloadRequired = true
  }
}
