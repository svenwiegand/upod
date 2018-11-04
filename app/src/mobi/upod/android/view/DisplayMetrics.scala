package mobi.upod.android.view

import android.content.Context

class DisplayMetrics(context: Context) {
  val metrics = context.getResources.getDisplayMetrics
  val density = metrics.density

  val displayHeight = metrics.heightPixels
  val displayWidth = metrics.widthPixels
  val smallestScreenDimension = math.min(displayHeight, displayWidth)
  val largestScreenDimension = math.max(displayHeight, displayWidth)

  def dpToPx(dp: Int): Int = (density * dp + 0.5f).toInt

  def pxToDp(px: Int): Int = (px / density).toInt
}

object DisplayMetrics {
  def apply(context: Context): DisplayMetrics =
    new DisplayMetrics(context)
}