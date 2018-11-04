package mobi.upod.android.view

import android.content.Context
import android.view.View

trait HeaderViews {

  protected def context: Context

  protected def hasHeaders: Boolean = false

  protected def onAddHeaders(): Unit = ()

  protected def addHeader(view: View): Unit

  protected def removeHeader(view: View): Unit

  protected def onHeaderLayoutChanged(): Unit
}
