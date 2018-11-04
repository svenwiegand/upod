package mobi.upod.android.widget

import android.content.Context
import android.util.AttributeSet
import android.view.View.MeasureSpec
import android.widget.FrameLayout
import mobi.upod.app.R

class VideoContainerLayout(context: Context, attributes: AttributeSet) extends FrameLayout(context, attributes) {
  import VideoContainerLayout._
  import VideoContainerLayout.Dimension._

  setBackgroundColor(0xff000000)

  private def fixedDimension: Dimension = {
    val xmlValues = getContext.obtainStyledAttributes(attributes, R.styleable.VideoContainerLayout)
    val dimensionId = xmlValues.getInt(R.styleable.VideoContainerLayout_fixed_dimension, Width.id)//This stuff doesn't work -- it always returns the default value
    Dimension(dimensionId)
  }

  override def onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int): Unit = {
    
    def measureSpec(size: Float): Int =
      MeasureSpec.makeMeasureSpec(size.toInt, MeasureSpec.EXACTLY)
    
    def setSize(width: Float, height: Float): Unit =
      super.onMeasure(measureSpec(width), measureSpec(height))
    
    val width = MeasureSpec.getSize(widthMeasureSpec)
    val height = MeasureSpec.getSize(heightMeasureSpec)
    
    if (fixedDimension == Width)
      setSize(width, width / AspectRatio)
    else
      setSize(height * AspectRatio, height)
  }
}

object VideoContainerLayout {
  val AspectRatio = 1920f / 1080

  object Dimension extends Enumeration {
    type Dimension = Value
    val Width = Value(1)
    val Height = Value(2)
  }
}
