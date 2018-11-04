package mobi.upod.android.widget

import android.content.Context
import android.view.{WindowManager, Gravity, View}
import android.graphics.Point

object Toast {

  def show(context: Context, text: CharSequence): Unit = 
    android.widget.Toast.makeText(context, text, android.widget.Toast.LENGTH_LONG).show()
  
  def show(context: Context, textId: Int): Unit =
    show(context, context.getString(textId))

  def showFor(view: View, text: CharSequence): Unit = {
    val toast = android.widget.Toast.makeText(view.getContext, text, android.widget.Toast.LENGTH_LONG)

    val windowManager = view.getContext.getSystemService(Context.WINDOW_SERVICE).asInstanceOf[WindowManager]
    val screenSize = new Point
    windowManager.getDefaultDisplay.getSize(screenSize)

    val viewLocation = new Array[Int](2)
    view.getLocationOnScreen(viewLocation)

    val xOffset = screenSize.x - viewLocation(0)
    val yOffset = screenSize.y - viewLocation(1)

    toast.setGravity(Gravity.BOTTOM|Gravity.RIGHT, xOffset.toInt, yOffset.toInt)
    toast.show()
  }
}
