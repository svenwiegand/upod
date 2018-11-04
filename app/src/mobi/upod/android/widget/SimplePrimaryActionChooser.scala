package mobi.upod.android.widget

import android.view.{MenuItem, Menu}

class SimplePrimaryActionChooser extends PrimaryActionChooser {

  override def choosePrimaryAction(menu: Menu): Option[MenuItem] = {

    def choose(index: Int): Option[MenuItem] = {
      // no way to access the item's showAsAction-flags, so we simply take the first enabled item with an icon
      val item = menu.getItem(index)
      if (item.isEnabled && item.isVisible && item.getIcon != null)
        Some(item)
      else if (index < (menu.size - 1))
        choose(index + 1)
      else
        None
    }

    choose(0)
  }
}
