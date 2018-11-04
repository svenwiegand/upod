package mobi.upod.android.widget

import android.support.v4.widget.DrawerLayout
import android.view.KeyEvent
import android.content.Context
import android.util.AttributeSet

class MainNavigationDrawerLayout(context: Context, attrs: AttributeSet) extends DrawerLayout(context, attrs) {
  
  override def onKeyDown(keyCode: Int, event: KeyEvent): Boolean = {
    if (keyCode == KeyEvent.KEYCODE_BACK)
      false
    else
      super.onKeyDown(keyCode, event)
  }

  override def onKeyUp(keyCode: Int, event: KeyEvent): Boolean = {
    if (keyCode == KeyEvent.KEYCODE_BACK)
      false
    else
      super.onKeyUp(keyCode, event)
  }
}
