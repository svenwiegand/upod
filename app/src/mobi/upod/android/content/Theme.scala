package mobi.upod.android.content

import android.content.Context
import android.content.res.Resources
import java.lang.String

import android.util.TypedValue
import mobi.upod.android.graphics.Color

class Theme(val context: Context) extends AnyVal {

  def getThemeResource(attr: Int): Int = {
    val styledAttributes = context.obtainStyledAttributes(Array(attr))
    val resId = styledAttributes.getResourceId(0, 0)
    styledAttributes.recycle()
    if (resId == 0)
      throw new Resources.NotFoundException
    else
      resId
  }

  def getThemeColor(attr: Int): Color =
    Color(context.getResources.getColor(getThemeResource(attr)))

  def getThemeDimension(attr: Int): Int =
    fromThemeValue(attr, v => TypedValue.complexToDimensionPixelSize(v.data, context.getResources.getDisplayMetrics))

  private def fromThemeValue[A](attr: Int, fetch: TypedValue => A): A = {
    val value = new TypedValue
    if (context.getTheme.resolveAttribute(attr, value, true))
      fetch(value)
    else
      throw new Resources.NotFoundException
  }
}

object Theme {

  def apply(context: Context): Theme = new Theme(context)

  implicit def context2Theme(context: Context): Theme = new Theme(context)
}