package mobi.upod.android.widget

import android.content.Context
import android.util.AttributeSet
import android.view.View.MeasureSpec
import android.widget.FrameLayout

class SquareFrameByHeightLayout(context: Context, attributes: AttributeSet) extends FrameLayout(context, attributes) {

  override def onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    val width = MeasureSpec.getSize(widthMeasureSpec)
    val height = MeasureSpec.getSize(heightMeasureSpec)
    val size = if (height > width && width > 0) width else height
    val measureSpec = MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY)
    super.onMeasure(measureSpec, measureSpec)
  }
}
