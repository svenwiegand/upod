package mobi.upod.android.app.action

import android.app.Activity
import android.content.Context

trait ImplicitActivityContext extends ImplicitContext { self: Activity =>

  implicit def context: Context = this
}
