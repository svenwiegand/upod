package mobi.upod.android.app

import android.app.Activity
import android.os.Bundle

trait ActivityLifecycleTracker extends Activity {

  private var _created = false
  private var _started = false
  private var _running = false

  def isActivityCreated: Boolean = _created

  def isActivityStarted: Boolean = _started

  def isActivityRunning: Boolean = _running

  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)
    _created = true
  }

  override def onStart(): Unit = {
    super.onStart()
    _started = true
  }

  override def onResume(): Unit = {
    super.onResume()
    _running = true
  }

  override def onPause(): Unit = {
    _running = false
    super.onPause()
  }

  override def onStop(): Unit = {
    _started = false
    super.onStop()
  }

  override def onDestroy(): Unit = {
    _created = false
    super.onDestroy()
  }
}
