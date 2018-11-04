package mobi.upod.android.view

import android.view.View
import android.widget.CompoundButton
import android.widget.CompoundButton.OnCheckedChangeListener

object CompoundButtonCheckedChangeListener {

  def apply(handle: (View, Boolean) => Unit) = new OnCheckedChangeListener {
    override def onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean): Unit =
      handle(buttonView, isChecked)
  }

  def apply(handle: Boolean => Unit): OnCheckedChangeListener =
    apply((view, checked) => handle(checked))
}
