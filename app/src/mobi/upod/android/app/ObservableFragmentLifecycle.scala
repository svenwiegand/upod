package mobi.upod.android.app

import android.app.Fragment
import android.os.Bundle

trait ObservableFragmentLifecycle extends Fragment with ActivityLifecycle {
  private var _created = false
  private var _started = false
  private var _running = false

  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)
    _created = true
    fire(_.onActivityCreated())
  }

  override def onStart(): Unit = {
    super.onStart()
    _started = true
    fire(_.onActivityStart())
  }

  override def onResume(): Unit = {
    super.onResume()
    _running = true
    fire(_.onActivityResume())
  }

  override def onPause(): Unit = {
    _running = false
    fire(_.onActivityPause())
    super.onPause()
  }

  override def onStop(): Unit = {
    _started = false
    fire(_.onActivityStop())
    super.onStop()
  }

  override def onDestroy(): Unit = {
    _created = false
    fire(_.onActivityDestroyed())
    super.onDestroy()
  }

  protected def fireActiveState(listener: ActivityLifecycleListener) {
    if (_created) {
      listener.onActivityCreated()
    }
    if (_started) {
      listener.onActivityStart()
    }
    if (_running) {
      listener.onActivityResume()
    }
  }
}
