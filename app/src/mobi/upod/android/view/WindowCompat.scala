package mobi.upod.android.view

import android.view.Window
import mobi.upod.android.util.ApiLevel

sealed trait WindowCompat {
  def setStatusBarColor(window: Window, color: Int): Unit
}

object WindowCompat extends WindowCompat {
  private lazy val compat = if (ApiLevel >= ApiLevel.Lollipop) new MaterialWindowCompat else new LegacyWindowCompat

  override def setStatusBarColor(window: Window, color: Int): Unit = compat.setStatusBarColor(window, color)

  private final class LegacyWindowCompat extends WindowCompat {
    override def setStatusBarColor(window: Window, color: Int): Unit = ()
  }

  private final class MaterialWindowCompat extends WindowCompat {
    override def setStatusBarColor(window: Window, color: Int): Unit =
      window.setStatusBarColor(color)
  }
}
