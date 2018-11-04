package mobi.upod.android.widget

import android.view.View
import android.content.Context
import android.util.AttributeSet
import mobi.upod.android.view.DisplayUnits._
import mobi.upod.android.view.DisplayMetrics
import android.graphics.{Rect, Canvas, Paint}
import android.view.View.MeasureSpec
import mobi.upod.app.R

class PageIndicatorView(context: Context, attrs: AttributeSet) extends View(context, attrs) {
  private implicit val DisplayMetrics = new DisplayMetrics(context)
  private val radius = 5.dp.toPx
  private val diameter = 2 * radius
  private val gap = 5.dp.toPx
  private lazy val inactivePaint = createPaint(0x99333333)
  private lazy val activePaint = createPaint(context.getResources.getColor(R.color.primary))
  private var pageCount = 1
  private var currentPage = 0

  private def createPaint(color: Int): Paint = {
    val paint = new Paint(Paint.ANTI_ALIAS_FLAG)
    paint.setColor(color)
    paint.setStyle(Paint.Style.FILL)
    paint
  }

  def setPageCount(count: Int): Unit = {
    if (count != pageCount) {
      pageCount = count
      if (currentPage >= pageCount) {
        currentPage = pageCount - 1
      }
      invalidate()
    }
  }

  def setCurrentPage(page: Int): Unit = {
    if (page != currentPage) {
      require(page >= 0 && page <= pageCount)
      currentPage = page
      invalidate()
    }
  }

  private def drawingWidth: Int =
    pageCount * diameter + (pageCount - 1) * gap

  private def drawingHeight: Int =
    diameter

  override def onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int): Unit = {
    def measureSpec(size: Int): Int =
      MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY)

    val width = drawingWidth + getPaddingLeft + getPaddingRight
    val height = drawingHeight + getPaddingTop + getPaddingBottom
    super.onMeasure(measureSpec(width), measureSpec(height))
  }

  override def onDraw(canvas: Canvas): Unit = {
    val rect = new Rect()
    getDrawingRect(rect)

    val x = (rect.width() - drawingWidth) / 2 + radius
    val y = (rect.height() - drawingHeight) / 2 + radius
    for (i <- 0 until pageCount) {
      canvas.drawCircle(x + i * (diameter + gap), y, if (i == currentPage) radius else radius / 2, if (i == currentPage) activePaint else inactivePaint)
    }

    super.onDraw(canvas)
  }
}
