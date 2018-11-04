package mobi.upod.app.gui.playback

import android.view.View

trait ActivatableIndicator extends View {

  def setActive(active: Boolean): Unit
}
