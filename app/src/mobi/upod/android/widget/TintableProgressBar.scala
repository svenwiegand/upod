package mobi.upod.android.widget

import android.graphics._
import android.graphics.drawable._
import android.view.Gravity
import android.widget.ProgressBar
import mobi.upod.android.view.Tintable
import mobi.upod.app.R

trait TintableProgressBar extends ProgressBar with Tintable {
  private val progressDrawable = new ProgressDrawable(0xff)
  private val secondaryProgressDrawable = new ProgressDrawable(0x77)
  protected lazy val trackSize: Int = getResources.getDimension(R.dimen.progress_bar_size).toInt
  protected def horizontal: Boolean

  initDrawable()

  private def initDrawable(): Unit = {

    def clip(drawable: Drawable): ClipDrawable = new ClipDrawable(
      drawable,
      if (horizontal) Gravity.LEFT else Gravity.BOTTOM,
      if (horizontal) ClipDrawable.HORIZONTAL else ClipDrawable.VERTICAL
    )

    def initProgressDrawable(): Unit = {
      val layers = new LayerDrawable(Array(new ColorDrawable(0), clip(secondaryProgressDrawable), clip(progressDrawable)))
      layers.setId(0, android.R.id.background)
      layers.setId(1, android.R.id.secondaryProgress)
      layers.setId(2, android.R.id.progress)

      setProgressDrawable(layers)
    }

    def initIndeterminateDrawable(): Unit =
      getIndeterminateDrawable.mutate

    initProgressDrawable()
    initIndeterminateDrawable()
  }

  def setTint(color: Int): Unit = {
    progressDrawable.setColor(color)
    secondaryProgressDrawable.setColor(color)
    Tintable.tint(getIndeterminateDrawable, color)
    invalidate()
  }

  def progressTrackHeight: Int = trackSize

  private class ProgressDrawable(baseAlpha: Int) extends Drawable {
    private var alpha = baseAlpha
    private val paint: Paint = new Paint()

    setColor(getResources.getColor(R.color.primary))

    def setColor(c: Int): Unit = {
      paint.setColor(c)
      paint.setAlpha(alpha)
    }

    override def draw(canvas: Canvas): Unit = {

      def horizontalRect(rect: Rect): Rect = {
        val offset = rect.height() / 2 - trackSize / 2
        new Rect(rect.left, rect.top + offset, rect.right, rect.bottom - offset)
      }

      def verticalRect(rect: Rect): Rect = {
        val offset = rect.width() / 2 - trackSize / 2
        new Rect(rect.left + offset, rect.top, rect.right - offset, rect.bottom)
      }

      val rect = getBounds
      val progressRect = if (horizontal) horizontalRect(rect) else verticalRect(rect)
      canvas.drawRect(progressRect, paint)
    }

    override def setColorFilter(cf: ColorFilter): Unit =
      paint.setColorFilter(cf)

    override def setAlpha(alpha: Int): Unit = {
      val base = baseAlpha.toFloat / 0xff
      val f = alpha.toFloat / 0xff
      this.alpha = (base * f * 0xff).toInt
      paint.setAlpha(this.alpha)
    }

    override def getOpacity: Int =
      PixelFormat.TRANSLUCENT
  }
}
