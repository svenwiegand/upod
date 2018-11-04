package mobi.upod.android.app

import android.app.Fragment

trait ListenerFragment extends Fragment with MultiWeakListener {

  override def onStart() {
    super.onStart()
    registerListener()
  }

  override def onStop() {
    unregisterListener()
    super.onStop()
  }
}
