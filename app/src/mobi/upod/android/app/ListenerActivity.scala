package mobi.upod.android.app

import android.app.Activity

trait ListenerActivity extends Activity with MultiWeakListener {

  override def onStart() {
    super.onStart()
    registerListener()
  }

  override def onStop() {
    unregisterListener()
    super.onStop()
  }
}
