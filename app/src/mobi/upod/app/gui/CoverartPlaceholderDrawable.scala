package mobi.upod.app.gui

import android.graphics._
import android.graphics.drawable.Drawable
import android.text.TextPaint
import mobi.upod.android.graphics.Color
import mobi.upod.app.data.PodcastColors

class CoverartPlaceholderDrawable extends Drawable {
  import mobi.upod.app.gui.CoverartPlaceholderDrawable._

  private val backgroundPaint = createBackgroundPaint
  private val textPaint = createTextPaint
  private var initials = ""

  private def createBackgroundPaint: Paint =
    new Paint()

  private def createTextPaint: TextPaint = {
    val paint = new TextPaint()
    paint.setAntiAlias(true)
    paint.setTextAlign(Paint.Align.CENTER)
    paint.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL))
    paint
  }

  def set(title: String, colors: PodcastColors): Unit = {

    def updateInitials(): Unit = {
      val charPairs = (" " + title).zip(title)
      val wordStarts = charPairs filter { case (a, b) =>
        b.isLetter && !a.isLetter ||
          b.isUpper && !a.isUpper ||
          b.isDigit && !a.isDigit
      }
      val i = wordStarts.map { case (_, c) => c }
      initials = i.take(3).mkString
    }

    def updatePaints(): Unit = {
      val bgColor = colors.background
      backgroundPaint.setColor(bgColor)
      textPaint.setColor(if (bgColor.isLight) DarkTextColor else LightTextColor)
    }

    updateInitials()
    updatePaints()
    invalidateSelf()
  }

  private def drawBackground(canvas: Canvas, rect: Rect): Unit =
    canvas.drawRect(rect, backgroundPaint)

  private def drawInitial(canvas: Canvas, rect: Rect): Unit = {
    val textSize = rect.height / 3
    textPaint.setTextSize(textSize)
    canvas.drawText(initials, rect.centerX, rect.centerY + textSize/3, textPaint)
  }

  override def draw(canvas: Canvas): Unit = {
    val bounds = getBounds
    drawBackground(canvas, bounds)
    drawInitial(canvas, bounds)
  }

  override def setColorFilter(cf: ColorFilter): Unit = {
    backgroundPaint.setColorFilter(cf)
    textPaint.setColorFilter(cf)
  }

  override def setAlpha(alpha: Int): Unit = {
    backgroundPaint.setAlpha(alpha)
    textPaint.setAlpha(alpha)
  }

  override def getOpacity: Int =
    PixelFormat.OPAQUE
}

private object CoverartPlaceholderDrawable {
  private val LightTextColor = Color(0xffffffff)
  private val DarkTextColor = Color(0xff212121)
}