package mobi.upod.android.widget

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.view.View.MeasureSpec
import android.widget.FrameLayout
import mobi.upod.app.R

/** A FrameLayout that keeps the specified width and sets the height to be at maximum as large as the width. */
class MaxSquareFrameByWidthLayout(context: Context, attributes: AttributeSet) extends FrameLayout(context, attributes) {
  private val maxSize: Int = {
    val a: TypedArray = context.obtainStyledAttributes(attributes, R.styleable.MaxSquareFrameByWidthLayout)
    val size = a.getDimensionPixelSize(R.styleable.MaxSquareFrameByWidthLayout_maxSize, Int.MaxValue)
    a.recycle()
    size
  }

  override def onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    val width = MeasureSpec.getSize(widthMeasureSpec)
    val height = MeasureSpec.getSize(heightMeasureSpec)
    val newWidth = if (width > maxSize) maxSize else width
    val newHeight = if (newWidth > height && height > 0) height else newWidth

    val newWidthMeasureSpec = if (newWidth != width) MeasureSpec.makeMeasureSpec(newWidth, MeasureSpec.EXACTLY) else widthMeasureSpec
    val newHeightMeasureSpec = MeasureSpec.makeMeasureSpec(newHeight, MeasureSpec.EXACTLY)
    super.onMeasure(newWidthMeasureSpec, newHeightMeasureSpec)
    //setMeasuredDimension(widthMeasureSpec, newHeightMeasureSpec)
  }


}
