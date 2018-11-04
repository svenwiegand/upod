package mobi.upod.app
package gui

import android.app.Activity
import android.app.LoaderManager.LoaderCallbacks
import android.content.{Context, Intent, Loader}
import android.os.Bundle
import android.view.Menu
import mobi.upod.android.app.{ViewMode, _}
import mobi.upod.android.content.AsyncDaoItemLoader
import mobi.upod.android.content.IntentHelpers._
import mobi.upod.android.content.preferences.PreferenceChangeListener
import mobi.upod.android.logging.Logging
import mobi.upod.android.view.ChildViews
import mobi.upod.app.gui.episode.FullFeaturedEpisodeListHolderActivity
import mobi.upod.app.gui.auth.SignInActivity
import mobi.upod.app.gui.preference.{StartupWizardActivity, StoragePermissionRequestActivity, UpdateWizardActivity}
import mobi.upod.app.services.{EpisodeListener, EpisodeService}
import mobi.upod.app.storage.{InternalAppPreferences, UiPreferences}

final class MainActivity
  extends ActivityWithPlaybackPanelAndStandardActions
  with StoragePermissionRequestActivity
  with ActivityLifecycleTracker
  with NavigationDrawer
  with FullFeaturedEpisodeListHolderActivity
  with ListenerActivity
  with EpisodeListener
  with LicenseUi
  with SignInActivity
  with LoaderCallbacks[Option[EpisodeCounterItem]]
  with ChildViews
  with AppInjection
  with Logging {

  protected val observables = Traversable(inject[EpisodeService])
  private lazy val internalAppPreferences = inject[InternalAppPreferences]
  private lazy val uiPreferences = inject[UiPreferences]

  // drawer stuff
  protected val isRootActivity = true
  protected val mainViewContainerId = R.id.main_view
  protected val drawerLayoutId = R.id.drawer_layout
  protected val drawerViewId = R.id.drawer
  protected val appTitleId = R.string.app_name
  private var navigationItemsChanged = false

  protected def createFragment(navItem: NavigationItem, viewMode: ViewMode) =
    MainNavigation.createFragment(navItem, viewMode)

  // actions
  override protected val optionsMenuResourceId = R.menu.main_activity_actions

  protected def navigationItems = MainNavigation.items.filter { item => item.id match {
    case MainNavigation.`newEpisodes` => !uiPreferences.skipNew
    case MainNavigation.audioEpisodes | MainNavigation.videoEpisodes => uiPreferences.showMediaTypeFilter
    case MainNavigation.downloadedEpisodes => uiPreferences.showDownloadedFilter
    case _ => true
  }}

  override def getActivity: Activity = this

  override def onCreate(savedInstanceState: Bundle) {
    if (internalAppPreferences.showStartupWizard) {
      log.trace("showing startup wizard instead of main activity")
      StartupWizardActivity.startInsteadOf(this)
      super.onCreate(savedInstanceState)
    } else if (UpdateWizardActivity.shouldBeShown) {
      log.trace("showing update wizard instead of main activity")
      UpdateWizardActivity.startInsteadOf(this)
      super.onCreate(savedInstanceState)
    } else {
      super.onCreate(savedInstanceState)
      setContentView(R.layout.main)
      createDrawer(savedInstanceState)
      createPlaybackPanel()
      getLoaderManager.initLoader(0, null, this)
    }
  }

  override def onStart() = {
    super.onStart()

    uiPreferences.skipNew.addWeakListener(SectionVisibilityChangeListener)
    uiPreferences.showMediaTypeFilter.addWeakListener(SectionVisibilityChangeListener)
    uiPreferences.showDownloadedFilter.addWeakListener(SectionVisibilityChangeListener)

    if (internalAppPreferences.openDrawerOnStart) {
      openDrawer()
      internalAppPreferences.openDrawerOnStart := false
    }
    if (!isDrawerOpen) {
      showFragmentTips()
    }
    
    if (navigationItemsChanged) {
      invalidateNavigationItems()
    }

    signInIfNecessary()
    syncService.ensureAutomaticSyncIsScheduled()
  }

  override def onStop(): Unit = {
    uiPreferences.skipNew.removeListener(SectionVisibilityChangeListener)
    uiPreferences.showMediaTypeFilter.removeListener(SectionVisibilityChangeListener)
    uiPreferences.showDownloadedFilter.removeListener(SectionVisibilityChangeListener)

    super.onStop()
  }

  override def onDestroy(): Unit = {
    super.onDestroy()

    if (isFinishing) {
      mobi.upod.android.database.Cursor.logOpenCursors()
    }
  }

  override protected def onDrawerClosed(): Unit = {
    showFragmentTips()
  }

  private def showFragmentTips(): Unit = {
    Option(getFragmentManager.findFragmentById(R.id.main_view)) match {
      case Some(fragment: UsageTips) =>
        getWindow.getDecorView.post(fragment.showUsageTips())
      case _ =>
    }
  }

  override protected def onCreateDrawerOptionsMenu(menu: Menu) = {
    getMenuInflater.inflate(R.menu.main_activity_actions, menu)
    prepareMenu(menu)
    true
  }

  def closeListView(): Unit = {
    if (!isDrawerOpen) {
      openDrawer()
    }
  }
  
  private def invalidateNavigationItems(): Unit = {
    navigationItemsChanged = false
    invalidateItems()
  }

  //
  // loading counters
  //

  def onCreateLoader(id: Int, args: Bundle): Loader[Option[EpisodeCounterItem]] =
    AsyncDaoItemLoader(this, episodeDao.findCounterItem)

  def onLoadFinished(loader: Loader[Option[EpisodeCounterItem]], data: Option[EpisodeCounterItem]): Unit = {
    import mobi.upod.app.gui.MainNavigation._

    val counters = data.getOrElse(EpisodeCounterItem(0, 0, 0, 0, 0, 0, 0, 0, 0))
    updateCounters(Map(
      newEpisodes -> counters.areNew,
      unfinishedEpisodes -> counters.unfinished,
      audioEpisodes -> counters.audio,
      videoEpisodes -> counters.video,
      downloadedEpisodes -> counters.downloaded,
      finishedEpisodes -> counters.finished,
      starred -> counters.starred,
      downloads -> counters.downloadQueue,
      playlist -> counters.playlist
    ))
  }

  def onLoaderReset(loader: Loader[Option[EpisodeCounterItem]]) {}

  override def onEpisodeCountChanged() {
    super.onEpisodeCountChanged()
    getLoaderManager.restartLoader(0, null, this)
  }

  //
  // preference change listener
  //

  private object SectionVisibilityChangeListener extends PreferenceChangeListener[Boolean] {

    override def onPreferenceChange(newValue: Boolean): Unit = {
      if (isActivityStarted)
        invalidateNavigationItems()
      else
        navigationItemsChanged = true
    }
  }
}

object MainActivity {

  def intent(context: Context, navItemId: Long, viewModeId: Int = 0): Intent = {
    val intent = new Intent(context, classOf[MainActivity])
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK)
    intent.putExtra(NavigationItemSelection, navItemId)
    intent.putExtra(ViewModeSelection, viewModeId)
    intent
  }

  def start(context: Context, navItemId: Long, viewModeId: Int = 0): Unit = {
    context.startActivity(intent(context, navItemId, viewModeId))
  }
}
