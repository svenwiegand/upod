package mobi.upod.android.view

import android.graphics.{PorterDuffColorFilter, ColorFilter, PorterDuff}
import android.graphics.drawable.Drawable
import android.widget.{TextView, ImageView}

trait Tintable {

  def setTint(color: Int): Unit
}

object Tintable {

  def tintOrIgnore(obj: Any, color: Int): Unit = obj match {
    case o: Tintable => o.setTint(color)
    case o: Drawable => tint(o, color)
    case o: ImageView => tint(o, color)
    case Some(o) => tintOrIgnore(o, color)
    case _ => // nothing we can do
  }

  def tint(d: Drawable, color: Int): Unit =
    d.setColorFilter(ColorFilter(color))

  def tint(v: ImageView, color: Int): Unit =
    v.setColorFilter(ColorFilter(color))

  def tint(v: TextView, color: Int): Unit = {
    v.setTextColor(color)
    v.getCompoundDrawables.filter(_ != null).foreach(tint(_, color))
  }

  def ColorFilter(color: Int): ColorFilter =
    new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_ATOP)
}