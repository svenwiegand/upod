package mobi.upod.android.app.action

import android.content.Context

trait AsyncActionHook {

  protected def preProcess(context: Context) {
    // do nothing by default
  }


  protected def postProcess(context: Context) {
    // do nothing by default
  }
}
