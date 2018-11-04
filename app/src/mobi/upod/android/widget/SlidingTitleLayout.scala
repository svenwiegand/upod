package mobi.upod.android.widget

import android.content.Context
import android.util.AttributeSet
import android.view.{GestureDetector, MotionEvent}
import android.widget.{FrameLayout, Scroller}
import mobi.upod.android.logging.Logging
import mobi.upod.android.view.ChildViews

class SlidingTitleLayout(context: Context, attrs: AttributeSet)
  extends FrameLayout(context, attrs)
  with ChildViews
  with Logging {

  lazy val titleView = getChildAt(0)
  lazy val contentView = getChildAt(1)
  private var listener: Option[SlidingTitleLayout.OnScrollListener] = None
  private lazy val gestureDetector = new GestureDetector(getContext, GestureListener)

  def setOnScrollListener(listener: Option[SlidingTitleLayout.OnScrollListener]): Unit =
    this.listener = listener

  override def shouldDelayChildPressedState() =
    true

  override def onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
    require(getChildCount == 2, "the only children should be the title and content")

    val width = right - left
    val titleHeight = titleView.getMeasuredHeight
    titleView.layout(0, 0, width, titleHeight)
    contentView.layout(0, titleHeight, width, titleHeight + contentView.getMeasuredHeight)
  }

  private def viewPortOffset: Int =
    titleView.getTranslationY.toInt

  def showFullTitle(): Unit = {
    scrollToY(0)
  }

  private def scrollToY(y: Int): Unit = {

    def setViewportOffset(offset: Int): Unit = {
      titleView.setTranslationY(offset)
      contentView.setTranslationY(offset)
      listener.foreach(_.onTitleOffset(offset))
    }

    val titleHeight = titleView.getHeight
    val scrollY = math.max(0, y)
    val offset = -math.min(scrollY, titleHeight)
    val contentScroll = scrollY + offset
    setViewportOffset(offset)
    contentView.setScrollY(contentScroll)
  }

  private def scrollPositionY: Int =
    -viewPortOffset + contentView.getScrollY

  private def scrollPositionX: Int =
    contentView.getScrollX

  override def scrollTo(x: Int, y: Int): Unit = {
    scrollToY(y)
    contentView.setScrollX(x)
  }

  override def onInterceptTouchEvent(ev: MotionEvent): Boolean =
    gestureDetector.onTouchEvent(ev)

  override def onTouchEvent(event: MotionEvent): Boolean =
    gestureDetector.onTouchEvent(event)

  //
  // gesture listener
  //

  private object GestureListener extends GestureDetector.SimpleOnGestureListener with Runnable {
    private val scroller = new Scroller(getContext)

    override def onDown(e: MotionEvent): Boolean = {
      scroller.forceFinished(true)
      false
    }

    override def onScroll(e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean = {
      scroller.forceFinished(true)
      scrollTo(scrollPositionX + distanceX.toInt, scrollPositionY + distanceY.toInt)
      true
    }

    override def onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean = {
      scroller.forceFinished(true)
      scroller.fling(scrollPositionX, scrollPositionY, -velocityX.toInt, -velocityY.toInt, 0, Int.MaxValue, 0, Int.MaxValue)
      scheduleScrollUpdate()
      true
    }

    private def scheduleScrollUpdate(): Unit =
      SlidingTitleLayout.this.post(this)

    override def run(): Unit = {
      if (scroller.computeScrollOffset()) {
        val prevScrollX = scrollPositionX
        val prevScrollY = scrollPositionY
        scrollTo(scroller.getCurrX, scroller.getCurrY)

        if (scrollPositionX != prevScrollX || scrollPositionY != prevScrollY)
          scheduleScrollUpdate()
        else
          scroller.forceFinished(true)
      }
    }
  }
}

object SlidingTitleLayout {

  trait OnScrollListener {
    def onTitleOffset(offset: Int): Unit
  }
}