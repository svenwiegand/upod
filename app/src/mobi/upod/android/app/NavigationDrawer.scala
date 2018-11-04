package mobi.upod.android.app

import android.app.Fragment
import android.content.res.Configuration
import android.os.Bundle
import android.support.v4.view.{GravityCompat, KeyEventCompat}
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.{ActionBar, ActionBarActivity, ActionBarDrawerToggle}
import android.view._
import android.widget.ListView
import com.escalatesoft.subcut.inject.Injectable
import mobi.upod.android.app.FragmentTransactions._
import mobi.upod.android.content.IntentHelpers._
import mobi.upod.android.logging.Logging
import mobi.upod.android.view.ChildViews
import mobi.upod.android.widget.AdapterViewClickListener
import mobi.upod.app.R

import scala.reflect.ClassTag

trait NavigationDrawer
  extends ActionBarActivity
  with ActivityStateHolder
  with ActionModeTracking
  with Injectable
  with ChildViews
  with ActionBar.OnNavigationListener
  with Logging {

  private var title: CharSequence = ""

  private lazy val settings = inject[NavigationSettings]

  protected def isRootActivity: Boolean

  protected def drawerLayoutId: Int

  protected def drawerViewId: Int

  protected def appTitleId: Int

  protected def mainViewContainerId: Int

  protected def navigationItems: IndexedSeq[NavigationDrawerEntry]

  protected def createFragment(navItem: NavigationItem, viewMode: ViewMode): Fragment

  private var currentNavItem: Option[NavigationItem] = None

  private var currentViewModes: IndexedSeq[ViewMode] = IndexedSeq()

  private var currentViewMode: Option[ViewMode] = None

  private lazy val drawerLayout = optionalChildAs[DrawerLayout](drawerLayoutId)

  private var openedByBackButton = false

  private lazy val actionBarDrawerToggle = drawerLayout.map { drawerLayout =>
    new ActionBarDrawerToggle(
      this, drawerLayout, R.string.action_open_drawer, R.string.action_close_drawer) {

      override def onDrawerOpened(drawerView: View) {
        disableViewModeSelector()
        setTitle(appTitleId)
        invalidateOptionsMenu()
        NavigationDrawer.this.onDrawerOpened()
      }

      override def onDrawerClosed(drawerView: View) {
        openedByBackButton = false
        setTitle(currentNavItem.map(_.windowTitleId).getOrElse(appTitleId))
        enableViewModeSelectorIfApplicable()
        invalidateOptionsMenu()
        NavigationDrawer.this.onDrawerClosed()
      }
    }
  }

  private lazy val navList = childAs[ListView](drawerViewId)

  private lazy val adapter = new NavigationDrawerEntryAdapter(navigationItems)

  protected lazy val isDrawerCollapsible = drawerLayout.isDefined

  protected def onCreateDrawerOptionsMenu(menu: Menu): Boolean = false

  def isDrawerOpen = drawerLayout.map(_.isDrawerOpen(Gravity.LEFT)).getOrElse(true)

  def openDrawer() {
    drawerLayout.foreach(_.openDrawer(Gravity.LEFT))
  }

  def closeDrawer() {
    drawerLayout.foreach(_.closeDrawer(Gravity.LEFT))
  }

  protected def onDrawerOpened(): Unit = {}

  protected def onDrawerClosed(): Unit = {}

  protected def createDrawer(savedInstanceState: Bundle) {
    def initDrawerList() {
      navList.setAdapter(adapter)
      navList.setOnItemClickListener(AdapterViewClickListener(index => onDrawerNavigationItemSelected(index)))
    }

    def initDrawerLayout() {
      drawerLayout.foreach {
        layout =>
          layout.setDrawerShadow(R.drawable.pane_shadow_right_4, GravityCompat.START)
          layout.setDrawerListener(actionBarDrawerToggle.get)
      }
    }

    def initActionBar() {
      if (isRootActivity && isDrawerCollapsible) {
        getSupportActionBar.setDisplayHomeAsUpEnabled(true)
        getSupportActionBar.setHomeButtonEnabled(true)
      }
    }

    def restoreState(): Unit = {

      def get[A](key: String)(implicit classTag: ClassTag[A]): A = {
        try {
          val result = savedInstanceState.getSerializable(key).asInstanceOf[A]
          if (result == null)
            throw new NoSuchElementException(key)
          else
            result
        } catch {
          case ex: ClassCastException =>
            log.error(s"element '$key' is not of expected type", ex)
            throw new NoSuchElementException(s"element '$key' is not of expected type")
        }
      }

      if (savedInstanceState != null) {
        try {
          currentViewModes = get[Array[ViewMode]]("currentViewModes").toIndexedSeq
          currentNavItem = get[Option[NavigationItem]]("currentNavItem")
          currentViewMode = get[Option[ViewMode]]("currentViewMode")
          setViewModes(currentViewModes)
        } catch {
          case ex: NoSuchElementException =>
            log.error("unexpected saved instance state", ex)
        }
      }
    }

    def restoreNavigation() {

      def intentNavigationItem: Option[NavigationItem] =
        adapter.items collect { case item: NavigationItem => item } find (_.id == getIntent.getExtra(NavigationItemSelection))

      def intentViewMode(navItem: NavigationItem): Option[ViewMode] = {
        val viewModeId = getIntent.getExtra(ViewModeSelection)
        navItem.viewModesById.get(viewModeId)
      }

      val intentNavItem = intentNavigationItem
      log.debug(s"intentNavitem = $intentNavItem")
      intentNavItem match {
        case Some(navItem) =>
          selectDrawerNavigationItem(navItem, true)
          val viewMode = intentViewMode(navItem)
          selectViewMode(viewMode)
        case None =>
          val navigationTarget = settings.navigationTarget
          selectDrawerNavigationItem(navigationTarget.item, true)
          val viewMode = Some(navigationTarget.viewMode)
          selectViewMode(viewMode)
      }
    }

    initDrawerList()
    initDrawerLayout()
    initActionBar()
    restoreState()
    restoreNavigation()
  }

  override def onSaveInstanceState(outState: Bundle): Unit = {
    super.onSaveInstanceState(outState)
    outState.putSerializable("currentNavItem", currentNavItem)
    outState.putSerializable("currentViewMode", currentViewMode)
    outState.putSerializable("currentViewModes", currentViewModes.toArray)
  }

  private def updateSettings() {
    if (currentNavItem.isDefined && currentViewMode.isDefined) {
      settings.navigationTarget = NavigationTarget(currentNavItem.get, currentViewMode.get)
    }
  }

  private def onDrawerNavigationItemSelected(index: Int): Unit = {
    adapter.items(index) match {
      case item: NavigationItem =>
        onDrawerNavigationItemSelected(Some(item))
      case item: NavigationActionItem =>
        item.action.fire(this)
        currentNavItem.foreach(selectDrawerNavigationItem(_))
      case _ => // ignore (should not happen)
    }
    drawerLayout foreach { layout =>
      layout.closeDrawers()
    }
  }

  private def onDrawerNavigationItemSelected(navItem: Option[NavigationItem]) {
    if (currentNavItem != navItem) {
      log.crashLogInfo(s"changing to drawer item $navItem")
      currentNavItem = navItem
      currentViewMode = None
      setViewModes(navItem.map(_.viewModes).getOrElse(IndexedSeq()))
    }
  }

  private def selectDrawerNavigationItem(navItem: NavigationItem, ensureVisible: Boolean = false) {
    log.debug(s"selecting drawer item ${navItem.id}")
    val itemIndex = adapter.items.indexWhere {
      case item: NavigationItem if item.id == navItem.id => true
      case _ => false
    }
    navList.setItemChecked(itemIndex, true)
    if (ensureVisible) {
      navList.setSelection(itemIndex)
    }
    onDrawerNavigationItemSelected(Some(navItem))
  }

  override def onPostCreate(savedInstanceState: Bundle) {
    super.onPostCreate(savedInstanceState)

    // Sync the toggle state after onRestoreInstanceState has occurred.
    actionBarDrawerToggle.foreach { _.syncState() }
  }

  override def onConfigurationChanged(newConfig: Configuration) {
    super.onConfigurationChanged(newConfig)

    actionBarDrawerToggle.foreach { _.onConfigurationChanged(newConfig) }
  }


  override def onCreateOptionsMenu(menu: Menu): Boolean = {
    val superResult = super.onCreateOptionsMenu(menu)
    if (drawerLayout.isDefined && drawerLayout.get.isDrawerOpen(Gravity.LEFT)) {
      menu.clear()
      onCreateDrawerOptionsMenu(menu) || superResult
    } else {
      superResult
    }
  }

  override def onPrepareOptionsMenu(menu: Menu) = {
    if (drawerLayout.isDefined && drawerLayout.get.isDrawerOpen(Gravity.LEFT)) {
      menu.clear()
      onCreateDrawerOptionsMenu(menu)
    } else {
      super.onPrepareOptionsMenu(menu)
    }
  }

  override def onOptionsItemSelected(item: MenuItem): Boolean = {
    // Pass the event to ActionBarDrawerToggle, if it returns
    // true, then it has handled the app icon touch event
    if (isDrawerCollapsible && isRootActivity && actionBarDrawerToggle.get.onOptionsItemSelected(item)) {
      true
    } else {
      super.onOptionsItemSelected(item)
    }
  }

  //
  // back key handling
  //

  override def onKeyDown(keyCode: Int, event: KeyEvent): Boolean = {
    if (keyCode == KeyEvent.KEYCODE_BACK) {
      KeyEventCompat.startTracking(event)
      true
    } else {
      super.onKeyDown(keyCode, event)
    }
  }

  override def onKeyUp(keyCode: Int, event: KeyEvent): Boolean = {
    if (keyCode == KeyEvent.KEYCODE_BACK && isDrawerCollapsible) {
      if (isDrawerOpen) {
        if (openedByBackButton)
          finish()
        else
          closeDrawer()
      } else {
        openedByBackButton = true
        openDrawer()
      }
      true
    } else {
      super.onKeyUp(keyCode, event)
    }
  }

  //
  // title handling and navigation mode
  //

  private def setViewModes(viewModes: IndexedSeq[ViewMode]) {
    currentViewModes = viewModes
    viewModes.size match {
      case 0 =>
        throw new IllegalArgumentException("At least one view mode is required")
      case 1 =>
        disableViewModeSelector()
        onViewModeSelected(Some(viewModes.head))
      case _ =>
        enableViewModeSelector()
    }
  }

  private def enableViewModeSelectorIfApplicable() {
    if (currentViewModes.size > 1) {
      enableViewModeSelector()
    }
  }

  private def enableViewModeSelector() {
    val viewModeNames = currentViewModes map { viewMode => getString(viewMode.titleId) }

    val actionBar = getSupportActionBar
    actionBar.setDisplayShowCustomEnabled(true)
    actionBar.setCustomView(new SubTitleNavigationSpinner(actionBar.getThemedContext, this, viewModeNames, this))
    selectViewMode(currentViewMode)

    super.setTitle("")
    setTitle(currentNavItem.map(navItem => getString(navItem.windowTitleId)).getOrElse(title))
  }

  private def disableViewModeSelector() {
    val actionBar = getSupportActionBar
    actionBar.setCustomView(null)
    actionBar.setDisplayShowCustomEnabled(false)

    setTitle(currentNavItem.map(navItem => getString(navItem.windowTitleId)).getOrElse(title))
  }

  /** Called when the user selects a view mode using the action bar's view mode picker
    */
  def onNavigationItemSelected(itemPosition: Int, itemId: Long) = {
    if (state.created) {
      onViewModeSelected(Some(currentViewModes(itemPosition)))
      true
    } else {
      false
    }
  }

  private def onViewModeSelected(viewMode: Option[ViewMode]) {
    if (viewMode != currentViewMode) {
      ensureActionModeFinished()
      currentViewMode = viewMode

      getFragmentManager.inTransactionAllowingStateLoss { trx =>
        Option(getFragmentManager.findFragmentById(mainViewContainerId)) foreach { trx.remove }
        val newFragment = currentViewMode map { viewMode => createFragment(currentNavItem.get, viewMode) }
        newFragment foreach { trx.add(mainViewContainerId, _)}
      }
      updateSettings()
      setTitle(currentNavItem.map(navItem => getString(navItem.windowTitleId)).getOrElse(title))
    }
  }

  private def selectViewMode(viewMode: Option[ViewMode]) {
    log.crashLogInfo(s"selecting view mode $viewMode")
    (viewMode, getSupportActionBar.getCustomView) match {
      case (Some(mode), spinner: SubTitleNavigationSpinner) =>
        spinner.setSelectedItem(currentViewModes.indexWhere(_.id == mode.id))
      case (None, spinner: SubTitleNavigationSpinner) if currentNavItem.isDefined =>
        selectViewMode(Some(settings.recentViewMode(currentNavItem.get)))
      case _ => // nothing we can do
    }
  }

  override def setTitle(title: CharSequence) {
    if (title != "") {
      this.title = title
    }
    getSupportActionBar.getCustomView match {
      case null => super.setTitle(title)
      case spinner: SubTitleNavigationSpinner => spinner.setTitle(title)
    }
  }

  def invalidateItems(): Unit = {
    val items = navigationItems

    def selectFirstItem(): Unit = {
      items.collectFirst { case item: NavigationItem => item } foreach(selectDrawerNavigationItem(_))
    }

    adapter.setItems(items)
    currentNavItem foreach { navItem =>
      if (!items.contains(navItem)) {
        selectFirstItem()
      }
    }
  }

  def updateCounters(countersByItemId: Map[Long, Int]) {
    adapter.updateCounters(countersByItemId)
  }
}
