package mobi.upod.android.app.action

import android.content.Context
import android.app.Fragment

trait ImplicitFragmentContext extends ImplicitContext { self: Fragment =>

  implicit def context: Context = getActivity
}
