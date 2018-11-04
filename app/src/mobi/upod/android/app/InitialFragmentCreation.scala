package mobi.upod.android.app

import android.app.Fragment
import android.os.Bundle

trait InitialFragmentCreation extends Fragment {
  // this field should be private, but then we get a strange runtime error (scala compiler problem?)
  protected var _initiallyCreated = false

  override def onActivityCreated(savedInstanceState: Bundle) {
    super.onActivityCreated(savedInstanceState)
    if (!_initiallyCreated) {
      onInitialActivityCreation()
      _initiallyCreated = true
    } else {
      onActivityRecreated(savedInstanceState)
    }
  }

  protected def onInitialActivityCreation() {
    // do nothing by default
  }

  protected def onActivityRecreated(savedInstanceState: Bundle) {
    // do nothing by default
  }
}
