package mobi.upod.android.widget

import android.content.Context
import android.util.AttributeSet
import android.webkit.WebView

class WebViewWithClampedScrollPositions(context: Context, attrs: AttributeSet) extends WebView(context, attrs) {

  override def scrollTo(x: Int, y: Int): Unit = {
    val maxX = computeHorizontalScrollRange() - getWidth
    val maxY = computeVerticalScrollRange() - getHeight
    super.scrollTo(math.min(x, maxX), math.min(y, maxY))
  }
}