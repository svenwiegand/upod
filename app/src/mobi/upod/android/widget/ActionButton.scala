package mobi.upod.android.widget

import android.content.Context
import android.util.AttributeSet
import android.widget.Button

class ActionButton(protected val context: Context, protected val attrs: AttributeSet)
  extends Button(context, attrs)
  with AbstractActionButton {

  override def getBaseline: Int =
    super.getHeight / 2
}
