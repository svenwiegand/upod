package mobi.upod.app.gui

import android.support.v4.view.MenuItemCompat
import android.support.v7.app.ActionBarActivity
import android.view.{KeyEvent, Menu}
import com.escalatesoft.subcut.inject.Injectable
import mobi.upod.android.logging.Logging
import mobi.upod.app.R
import mobi.upod.app.gui.cast.MediaRouteActionProvider
import mobi.upod.app.services.cast.MediaRouteService

trait MediaRouteUi extends ActionBarActivity with Injectable with Logging {
  private lazy val mediaRouteService = inject[MediaRouteService]

  override def onCreateOptionsMenu(menu: Menu): Boolean = {
    super.onCreateOptionsMenu(menu)

    Option(menu.findItem(R.id.action_media_route)) foreach { mediaRouteItem =>
      val mediaRouteActionProvider = MenuItemCompat.getActionProvider(mediaRouteItem).asInstanceOf[MediaRouteActionProvider]
      mediaRouteActionProvider.setRouteSelector(mediaRouteService.selector)
    }
    true
  }

  override def dispatchKeyEvent(event: KeyEvent): Boolean = mediaRouteService.currentDevice match {
    case Some(device) if event.getKeyCode == KeyEvent.KEYCODE_VOLUME_UP && event.getAction == KeyEvent.ACTION_DOWN =>
      device.increaseVolume()
      true
    case Some(device) if event.getKeyCode == KeyEvent.KEYCODE_VOLUME_DOWN && event.getAction == KeyEvent.ACTION_DOWN =>
      device.decreaseVolume()
      true
    case _ =>
      super.dispatchKeyEvent(event)
  }
}
