package mobi.upod.android.widget

import android.content.Context
import android.graphics._
import android.graphics.drawable.{Drawable, StateListDrawable}
import android.util.AttributeSet
import android.widget.SeekBar
import mobi.upod.android.util.ApiLevel
import mobi.upod.app.R

class TintableSeekBar(context: Context, attrs: AttributeSet) extends SeekBar(context, attrs) with TintableProgressBar {
  private val thumb = if (ApiLevel >= ApiLevel.Lollipop) new MaterialThumb else new LegacyThumb

  override protected def horizontal = true

  override protected lazy val trackSize = getResources.getDimension(R.dimen.seek_bar_track_size).toInt

  override def setTint(color: Int): Unit = {
    setTrackTint(color)
    setThumbTint(color)
  }

  def setTrackTint(color: Int): Unit =
    super.setTint(color)

  def setThumbTint(color: Int): Unit =
    thumb.setTint(color)

  private trait Thumb {
    def setTint(color: Int)
  }

  final private class MaterialThumb extends Thumb {
    getThumb.mutate()

    override def setTint(color: Int): Unit =
      getThumb.setTint(color)
  }

  final private class LegacyThumb extends Thumb {
    private val maxThumbSize = context.getResources.getDimension(R.dimen.seek_bar_thumb_click_size)
    private val defaultThumbDrawable = new ThumbDrawable(R.dimen.seek_bar_thumb_default_size, maxThumbSize)
    private val clickedThumbDrawable = new ThumbDrawable(R.dimen.seek_bar_thumb_click_size, maxThumbSize)

    initThumbDrawable()

    private def initThumbDrawable(): Unit = if (ApiLevel < ApiLevel.Lollipop) {
      val thumbDrawable = new StateListDrawable
      thumbDrawable.addState(Array(android.R.attr.state_enabled, android.R.attr.state_pressed), clickedThumbDrawable)
      thumbDrawable.addState(Array(android.R.attr.state_enabled), defaultThumbDrawable)
      thumbDrawable.addState(Array(), new ThumbDrawable(0, maxThumbSize))
      setThumb(thumbDrawable)
    }

    override def setTint(color: Int): Unit = {
      defaultThumbDrawable.setColor(color)
      clickedThumbDrawable.setColor(color)
    }

    private class ThumbDrawable(diameterResId: Int, maxRadius: Float) extends Drawable {
      private val paint: Paint = new Paint(Paint.ANTI_ALIAS_FLAG)
      private val radius = if (diameterResId > 0) getResources.getDimension(diameterResId) / 2 else 0f
      private val size = (2 * maxRadius).toInt

      setColor(getResources.getColor(R.color.primary))

      def setColor(c: Int): Unit =
        paint.setColor(c)

      override def draw(canvas: Canvas): Unit = {
        val rect = getBounds
        canvas.drawCircle(rect.centerX, rect.centerY, radius, paint)
      }

      override def getIntrinsicWidth: Int =
        size

      override def getIntrinsicHeight: Int =
        size

      override def setColorFilter(cf: ColorFilter): Unit =
        paint.setColorFilter(cf)

      override def setAlpha(alpha: Int): Unit =
        paint.setAlpha(alpha)

      override def getOpacity: Int =
        PixelFormat.TRANSLUCENT
    }
  }
}
