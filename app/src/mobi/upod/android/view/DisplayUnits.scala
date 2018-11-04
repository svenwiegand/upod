package mobi.upod.android.view

object DisplayUnits {

  implicit class Pixels(val pixels: Int) extends AnyVal {

    def dp = new DensityIndependentPixels(pixels)

    def px = new ScreenPixels(pixels)
  }

  class DensityIndependentPixels(val dp: Int) extends AnyVal {

    def px(implicit metrics: DisplayMetrics) = metrics.dpToPx(dp)

    def toPx(implicit metrics: DisplayMetrics) = px
  }

  class ScreenPixels(val px: Int) extends AnyVal {

    def dp(implicit metrics: DisplayMetrics) = metrics.pxToDp(px)

    def toDp(implicit metrics: DisplayMetrics) = dp
  }
}
