package mobi.upod.app.services

import android.app.Application
import mobi.upod.android.app.{NavigationItem, NavigationTarget, NavigationSettings}
import mobi.upod.app.gui.MainNavigation
import mobi.upod.app.storage.NavigationPreferences

class NavigationSettingsService(app: Application) extends NavigationSettings {
  private lazy val prefs = new NavigationPreferences(app, MainNavigation.findPodcasts, MainNavigation.viewModeIdPodcasts)

  override def navigationTarget: NavigationTarget = {
    MainNavigation.itemsById.getOrElse(prefs.navItem.get, MainNavigation.itemsById(MainNavigation.findPodcasts)) match {
      case item: NavigationItem =>
        NavigationTarget(item, item.viewModesById.getOrElse(prefs.viewMode, item.viewModes.head))
      case _ =>
        throw new IllegalArgumentException("invalid navigation item id")
    }
  }

  override def navigationTarget_=(target: NavigationTarget) {
    prefs.navItem := target.item.id
    prefs.viewMode := target.viewMode.id
    prefs.recentViewMode(target.item.id) := target.viewMode.id
  }

  def recentViewMode(navItem: NavigationItem) = navItem.viewModesById(prefs.recentViewMode(navItem.id))
}
