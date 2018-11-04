package mobi.upod.android.view

import android.support.v4.view.ViewPager
import android.util.AttributeSet
import android.content.Context
import android.view.MotionEvent

class NonSwipeableViewPager(context: Context, attrs: AttributeSet) extends ViewPager(context, attrs) {

  override def onInterceptTouchEvent(ev: MotionEvent) = false

  override def onTouchEvent(ev: MotionEvent) = false
}
