package mobi.upod.android.app.action

import android.content.Context

trait ImplicitContext {

  implicit def context: Context
}
