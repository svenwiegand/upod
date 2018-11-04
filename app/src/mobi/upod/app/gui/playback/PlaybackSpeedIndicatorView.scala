package mobi.upod.app.gui.playback

import android.widget.TextView
import android.util.AttributeSet
import android.content.Context
import android.graphics.{RectF, Paint, Rect, Canvas}
import mobi.upod.android.view.DisplayUnits._
import mobi.upod.android.view.{Tintable, DisplayMetrics}
import mobi.upod.app.R

class PlaybackSpeedIndicatorView(context: Context, attrs: AttributeSet)
  extends TextView(context, attrs)
  with Tintable
  with ActivatableIndicator {

  private val MaxSpeed = 2.5f
  private implicit val DisplayMetrics = new DisplayMetrics(context)
  private val Padding = 8.dp.toPx
  private val StrokeWidth = getResources.getDimension(R.dimen.seek_bar_track_size)
  private lazy val trackPaint = createArcPaint(0x11ffffff)
  private lazy val speedPaint = createArcPaint(context.getResources.getColor(R.color.primary))
  private lazy val speedPaintInactive = createArcPaint(0x77ffffff)
  private val standardTextColor = 0x77ffffff
  private var _speed = 1.0f
  private var _active = false
  private var _tint = context.getResources.getColor(R.color.primary)

  private def createArcPaint(color: Int): Paint = {
    val paint = new Paint(Paint.ANTI_ALIAS_FLAG)
    paint.setColor(color)
    paint.setStyle(Paint.Style.STROKE)
    paint.setStrokeWidth(StrokeWidth)
    paint
  }

  override def setTint(color: Int): Unit = {
    _tint = color
    speedPaint.setColor(color)
  }

  def setPlaybackSpeed(speed: Float): Unit = {
    setText(f"$speed%1.1f")
    _speed = speed
    setTextColor(if (speed == 1f) standardTextColor else _tint)
    invalidate()
  }

  def setActive(active: Boolean): Unit = {
    _active = active
    if (!active)
      setPlaybackSpeed(1f)
    else
      invalidate()
  }

  override def onDraw(canvas: Canvas): Unit = {
    super.onDraw(canvas)

    val rect = new Rect()
    getDrawingRect(rect)

    val arcRect = new RectF(Padding, Padding, rect.right - Padding, rect.bottom - Padding)
    canvas.drawArc(arcRect, 135, 270, false, trackPaint)

    val enclosingAngle = if (_speed < 1.0f)
      2 * (_speed - 0.5f) * 135
    else
      135 + (_speed - 1.0f) / (MaxSpeed - 1.0f) * 135
    canvas.drawArc(arcRect, 135, enclosingAngle, false, if (_active) speedPaint else speedPaintInactive)
  }
}
