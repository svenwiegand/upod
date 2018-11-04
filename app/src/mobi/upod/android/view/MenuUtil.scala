package mobi.upod.android.view

import android.view.{MenuItem, Menu}

object MenuUtil {

  def filter(menu: Menu, include: MenuItem => Boolean): Unit = {
    for (i <- (0 until menu.size()).reverse) {
      val item = menu.getItem(i)
      if (!include(item)) {
        menu.removeItem(item.getItemId)
      }
    }
  }
}
