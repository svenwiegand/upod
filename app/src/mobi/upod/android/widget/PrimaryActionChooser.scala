package mobi.upod.android.widget

import android.view.{MenuItem, Menu}

trait PrimaryActionChooser {

  def choosePrimaryAction(menu: Menu): Option[MenuItem]
}
