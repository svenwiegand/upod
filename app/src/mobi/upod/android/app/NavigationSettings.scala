package mobi.upod.android.app

trait NavigationSettings {

  var navigationTarget: NavigationTarget

  def recentViewMode(navItem: NavigationItem): ViewMode
}
