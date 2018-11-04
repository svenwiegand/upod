package mobi.upod.android.app

import android.app.Fragment
import android.os.Bundle

trait FragmentStateHolder extends Fragment {
  private var _state: ActivityState = ActivityState.Launched

  def state = _state

  override def onCreate(savedInstanceState: Bundle): Unit = {
    _state = ActivityState.Created
    super.onCreate(savedInstanceState)
  }

  override def onStart(): Unit = {
    _state = ActivityState.Started
    super.onStart()
  }

  override def onResume(): Unit = {
    _state = ActivityState.Running
    super.onResume()
  }

  override def onPause(): Unit = {
    _state = ActivityState.Paused
    super.onPause()
  }

  override def onStop(): Unit = {
    _state = ActivityState.Stopped
    super.onStop()
  }

  override def onDestroy(): Unit = {
    _state = ActivityState.Destroyed
    super.onDestroy()
  }
}
