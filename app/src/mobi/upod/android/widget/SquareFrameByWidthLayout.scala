package mobi.upod.android.widget

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import android.view.View.MeasureSpec

class SquareFrameByWidthLayout(context: Context, attributes: AttributeSet) extends FrameLayout(context, attributes) {

  override def onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    val width = MeasureSpec.getSize(widthMeasureSpec)
    val height = MeasureSpec.getSize(heightMeasureSpec)
    val size = if (width > height && height > 0) height else width
    val measureSpec = MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY)
    super.onMeasure(measureSpec, measureSpec)
  }
}
