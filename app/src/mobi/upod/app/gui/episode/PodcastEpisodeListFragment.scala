package mobi.upod.app.gui.episode

import java.net.URL

import android.app.LoaderManager.LoaderCallbacks
import android.content.res.Configuration
import android.content.{Context, Loader}
import android.graphics.drawable.{ColorDrawable, LayerDrawable}
import android.os.Bundle
import android.view._
import android.widget.AbsListView
import android.widget.AbsListView.OnScrollListener
import mobi.upod.android.app.{SimpleAlertDialogFragment, SupportActionBar}
import mobi.upod.android.app.action.ActionState.ActionState
import mobi.upod.android.app.action._
import mobi.upod.android.content.AsyncDaoItemLoader
import mobi.upod.android.content.IntentHelpers._
import mobi.upod.android.os.AsyncTask
import mobi.upod.android.view.{MenuUtil, WindowCompat}
import mobi.upod.android.widget._
import mobi.upod.app.R
import mobi.upod.app.data._
import mobi.upod.app.gui._
import mobi.upod.app.gui.podcast.{SharePodcastAction, SubscriptionSettingsAction}
import mobi.upod.app.services.subscription.SubscriptionService
import mobi.upod.app.services.sync.{ImageFetcher, PodcastColorExtractor}
import mobi.upod.app.storage.{EpisodeDao, ImageSize, PodcastDao, UiPreferences}
import mobi.upod.util.Cursor

trait PodcastEpisodeListFragment
  extends EpisodeListFragment
  with SimpleFragmentActions
  with ConfirmedActionProviderFragment
  with SupportActionBar {

  private val headerLoaderId = 1
  protected val subscriptionService = inject[SubscriptionService]
  protected val podcastDao = inject[PodcastDao]
  private lazy val uiPreferences = inject[UiPreferences]
  private val coverartLoader = inject[CoverartLoader]
  private lazy val headerLoaderCallback = new PodcastHeaderLoaderCallback
  private lazy val header = new PodcastListHeader
  private var scrollListener: Option[AbsListView.OnScrollListener] = None

  setRetainInstance(false)

  protected def podcast = getActivity.getIntent.getExtra(PodcastSelection).get

  protected def showPodcastHeader = !getActivity.getIntent.getExtra(SuppressPodcastHeader)

  protected def viewModeId = MainNavigation.viewModeIdPodcastEpisodes

  protected def sortAscending: Boolean =
    uiPreferences.sortEpisodesAscending.get

  protected def sortAscending_=(ascending: Boolean): Unit =
    uiPreferences.sortEpisodesAscending := ascending

  override protected def orderedAscending: Boolean =
    sortAscending

  protected val podcastActions: Map[Int, Action] = Map(
    R.id.action_share -> new SharePodcastAction(Some(podcast)),
    R.id.action_sort_episodes_asc -> new SortEpisodesAction(true),
    R.id.action_sort_episodes_desc -> new SortEpisodesAction(false),
    R.id.action_podcast_subscribe -> new SubscribeAction,
    R.id.action_podcast_unsubscribe -> new UnsubscribeAction,
    R.id.action_podcast_delete -> new ConfirmedAction(
      R.string.action_podcast_delete,
      getResources.getQuantityString(R.plurals.confirm_delete_podcast, 1),
      this, DeletePodcastAction.ActionTag),
    R.id.action_subscription_settings -> new SubscriptionSettingsAction(podcast),
    R.id.action_podcast_update_coverart -> new UpdateCoverartAction,
    R.id.action_podcast_sync_error -> new ShowSyncErrorAction
  )

  override def confirmedAction(tag: String): Action = tag match {
    case DeletePodcastAction.ActionTag => DeletePodcastAction
  }

  override def onCreateOptionsMenu(menu: Menu, inflater: MenuInflater): Unit = {
    super.onCreateOptionsMenu(menu, inflater)
    MenuUtil.filter(menu, !header.isHeaderAction(_))
  }

  final protected def loadEpisodes(dao: EpisodeDao): Cursor[EpisodeListItem] =
    loadEpisodes(dao, podcast, sortAscending)

  protected def loadEpisodes(dao: EpisodeDao, podcast: PodcastListItem, sortAscending: Boolean): Cursor[EpisodeListItem]

  override protected def hasHeaders = true

  protected def setOnScrollListener(l: Option[AbsListView.OnScrollListener]): Unit =
    scrollListener = l

  override protected def onAddHeaders() {
    addPodcastHeaderIfApplicable()
    super.onAddHeaders()
  }

  private def addPodcastHeaderIfApplicable() {
    if (showPodcastHeader) {
      addHeader(header.view)
      header.update(podcast.title, podcast.imageUrl, podcast.extractedOrGeneratedColors, None, None, podcast.categories, podcast.subscribed)
      getLoaderManager.initLoader(headerLoaderId, null, headerLoaderCallback)
    }
  }

  private def updateSubscriptionViewStatus(subscribe: Boolean) {
    val pc = podcast.copy(subscribed = subscribe)
    getActivity.getIntent.putExtra(PodcastSelection, pc)
    header.updateSubscriptionStatus(subscribe)
    getActivity.invalidateOptionsMenu()
  }

  private def onSubscribe() {
    updateSubscriptionViewStatus(true)
    val podcastUri = podcast.uri
    AsyncTask.execute(subscriptionService.subscribe(podcastUri))
  }

  private def onUnsubscribe() {
    updateSubscriptionViewStatus(false)
    val podcastUri = podcast.uri
    AsyncTask.execute(subscriptionService.unsubscribe(podcastUri))
  }

  override def reload() {
    super.reload()
    if (showPodcastHeader) {
      getLoaderManager.restartLoader(headerLoaderId, null, headerLoaderCallback)
    }
  }

  def onPodcastChanged(): Unit = {
    prepareListForLoading()
    reload()
  }

  override def onRefresh(): Unit =
    syncService.syncPodcast(podcast.uri)

  private class PodcastHeaderLoaderCallback extends LoaderCallbacks[Option[Podcast]] {
    def onCreateLoader(id: Int, args: Bundle) =
      AsyncDaoItemLoader(getActivity, podcastDao.find(podcast.id))

    def onLoadFinished(loader: Loader[Option[Podcast]], data: Option[Podcast]) {
      data match {
        case Some(podcast) =>
          header.update(podcast.title, podcast.imageUrl, podcast.extractedOrGeneratedColors, podcast.subTitleDifferentToTitle, podcast.longestOfDescriptionAndSubTitle, podcast.categories, podcast.subscribed)
        case None =>
          header.update("", None, PodcastColors(0), None, None, Set(), true)
      }
    }

    def onLoaderReset(loader: Loader[Option[Podcast]]) {
      header.hide()
    }
  }

  protected def onSortOrderChanging(): Unit =
    prepareListForLoading()

  private class SortEpisodesAction(ascending: Boolean) extends Action {

    override def state(context: Context): ActionState =
      if (sortAscending != ascending) ActionState.enabled else ActionState.gone

    override def onFired(context: Context): Unit = {
      sortAscending = ascending
      onSortOrderChanging()
      reload()
      getListView.setSelection(0)
    }
  }

  private class SubscribeAction extends Action {

    override def state(context: Context): ActionState.ActionState =
      if (podcast.subscribed) ActionState.gone else ActionState.enabled

    def onFired(context: Context) {
      onSubscribe()
    }
  }

  private class UnsubscribeAction extends Action {

    override def state(context: Context): ActionState.ActionState =
      if (podcast.subscribed) ActionState.enabled else ActionState.gone

    def onFired(context: Context) {
      onUnsubscribe()
    }
  }

  private class ShowSyncErrorAction extends Action {

    override def state(context: Context): ActionState =
      if (podcast.syncError.isDefined) ActionState.enabled else ActionState.gone

    override def onFired(context: Context): Unit = podcast.syncError foreach { error =>
      SimpleAlertDialogFragment.show(
        PodcastEpisodeListFragment.this,
        SimpleAlertDialogFragment.defaultTag,
        R.string.action_podcast_sync_error,
        error,
        neutralButtonTextId = Some(R.string.close)
      )
    }
  }

  private object DeletePodcastAction extends AsyncAction[PodcastListItem, Unit] with ActionWaitDialog {
    val ActionTag = "deletePodcast"

    override protected def getData(context: Context): PodcastListItem =
      podcast

    override protected def processData(context: Context, data: PodcastListItem): Unit = {
      subscriptionService.delete(data.uri)
    }

    override protected def postProcessData(context: Context, result: Unit): Unit = {
      super.postProcessData(context, result)
      if (PodcastEpisodeListFragment.this.state.created) {
        Option(getActivity).foreach(_.finish())
      }
    }
  }

  private class UpdateCoverartAction extends AsyncAction[PodcastListItem, PodcastListItem] with ActionWaitDialog {
    override protected def waitDialogMessageId: Int = R.string.wait_please

    override protected def getData(context: Context): PodcastListItem =
      podcast

    override protected def processData(context: Context, data: PodcastListItem): PodcastListItem = {
      data.imageUrl.foreach(new ImageFetcher().fetchImage)
      data.copy(colors = new PodcastColorExtractor().colorizePodcast(data.uri, data.imageUrl, true))
    }

    override protected def postProcessData(context: Context, result: PodcastListItem): Unit = {
      super.postProcessData(context, result)
      reload()
    }
  }

  private class PodcastListHeader extends ContextMenuActions with PrimaryActionChooser with OnScrollListener {
    private val MaxImageElevation = getActivity.getResources.getDimension(R.dimen.elevation_app_bar)

    val view = View.inflate(getActivity, R.layout.podcast_list_header, null).asInstanceOf[ViewGroup]
    private val titleContainer = view.childViewGroup(R.id.podcastTitleContainer)
    private val podcastImageView = titleContainer.childImageView(R.id.podcastImage)
    private val titleView = titleContainer.childTextView(R.id.podcastTitle)
    private val categoriesView = titleContainer.childTextView(R.id.podcastCategories)
    private val descriptionView = view.childTextView(R.id.podcastDescription)
    private val floatingActionButton = view.childAs[FloatingActionButton](R.id.floatingActionButton)
    private val actionPanel = view.childAs[ActionPanel](R.id.actionPanel)

    private val actionBarShadow = getActivity.getWindow.getDecorView.optionalChildAs[View](R.id.actionBarShadow)
    private val podcastImageBottomShadow = titleContainer.optionalChildAs[View](R.id.podcastImageShadowBottom)
    private val podcastTitleContainerShadow = view.optionalChildAs[View](R.id.podcastTitleContainerShadow)


    implicit val context: Context = view.getContext
    private val theme = new Theme(view.getContext)
    private val hideActionBar = !getActivity.getResources.getBoolean(R.bool.splitScreen)
    private val applyParalaxScrolling =
      getResources.getConfiguration.orientation == Configuration.ORIENTATION_PORTRAIT && !getResources.getBoolean(R.bool.largeScreen)
    private val actionBarHeight = theme.Dimensions.ActionBarSize
    private val actionBarShadowOffset = if (hideActionBar) actionBarHeight else 0

    private val coverartPlaceholderDrawable = new CoverartPlaceholderDrawable with CoverartLoaderFallbackDrawable
    private val actionBarDimDrawable = getActivity.getResources.getDrawable(R.drawable.dimmed_overlay_top)
    private val actionBarColorDrawable = new ColorDrawable()
    private val actionBarBackground = new LayerDrawable(Array(actionBarDimDrawable, actionBarColorDrawable))

    private var actionBarTitle: CharSequence = null
    private var actionBarSubTitle: CharSequence = null

    init()

    private def init(): Unit = {
      getListView.setOnScrollListener(this)
      supportActionBar.setBackgroundDrawable(actionBarBackground)
      actionPanel.listener = this
      actionPanel.primaryActionChooser = this
      invalidateActions()
    }

    def update(
      title: String,
      imageUrl: Option[URL],
      colors: PodcastColors,
      subTitle: Option[String],
      description: Option[String],
      categories: Set[Category],
      subscribed: Boolean): Unit = {

      def updateTitle(): Unit =
        titleView.setText(podcast.title)

      def updateImage(): Unit = {
        coverartPlaceholderDrawable.set(title, colors)
        coverartLoader.displayImage(podcastImageView, ImageSize.full, imageUrl, Some(coverartPlaceholderDrawable))
      }

      def updateTexts(): Unit = {
        descriptionView.setText(description orElse subTitle getOrElse "")
        val categoryString = categories.map(_.displayString(view.getContext)).filter(_.nonEmpty).map(_.get).mkString(", ")
        categoriesView.setText(categoryString)
      }

      def updateColors(): Unit = {
        val bgColor = colors.nonLightBackground
        titleContainer.setBackgroundColor(bgColor)
        actionBarColorDrawable.setColor(bgColor)
        WindowCompat.setStatusBarColor(getActivity.getWindow, bgColor.dimmed)

        val accent = colors.accentForNonLightBackground(theme)
        floatingActionButton.setColor(accent)

        val keyColor = colors.key.getOrElse(theme.Colors.Primary)
        setSwipeRefreshColorScheme(keyColor)
      }

      updateTitle()
      updateImage()
      updateTexts()
      updateColors()
      invalidateActions()
      view.show()
    }

    def hide(): Unit =
      view.hide()

    //
    // action stuff
    //

    def updateSubscriptionStatus(subscribed: Boolean): Unit =
      invalidateActions()

    def invalidateActions(): Unit =
      actionPanel.invalidateActions()

    def isHeaderAction(item: MenuItem): Boolean = {
      item.getIcon != null &&
        item.getItemId != R.id.action_share &&
        item.getItemId != R.id.action_mark_all_read &&
        item.getItemId != R.id.action_mark_all_finished
    }

    override protected def contextMenuResourceId: Int =
      PodcastEpisodeListFragment.this.optionsMenuResourceId

    override protected def createActions: Map[Int, Action] =
      PodcastEpisodeListFragment.this.actions

    override def onCreateMenu(menu: Menu, view: View): Unit = {
      super.onCreateMenu(menu, view)
      MenuUtil.filter(menu, isHeaderAction)
    }

    override def choosePrimaryAction(menu: Menu): Option[MenuItem] = {

      def findAvailableSubscribeItem(startIndex: Int = 0): Option[MenuItem] = {
        if (startIndex < menu.size) {
          val item = menu.getItem(startIndex)
          if (item.isEnabled && item.isVisible && item.getItemId == R.id.action_podcast_subscribe)
            Some(item)
          else
            findAvailableSubscribeItem(startIndex + 1)
        } else {
          None
        }
      }

      findAvailableSubscribeItem()
    }

    //
    // visual sugar
    //

    private def updateImage(headerVisible: Boolean): Unit = if (applyParalaxScrolling) {

      def lowerImage(lowerFactor: Float): Unit =
        podcastImageBottomShadow.foreach(_.setAlpha(lowerFactor))

      def updateImageParallax(imageHeight: Int, visibleImageHeight: Int): Unit =
        podcastImageView.setTranslationY((imageHeight - visibleImageHeight) / 2)

      val imageHeight = podcastImageView.getHeight
      val visibleImageHeight = podcastImageView.getGlobalVisibleRect() match {
        case Some(rect) if headerVisible => rect.height
        case _ => 0
      }
      updateImageParallax(imageHeight, visibleImageHeight)
      lowerImage(math.min((imageHeight - visibleImageHeight) / (4 * MaxImageElevation), 1f))
    }

    private def updateActionBar(headerVisible: Boolean): Unit = if (hideActionBar) {
      val titleYPos = titleView.getRelativeVisibleRect(getListView) match {
        case Some(rect) if headerVisible => rect.top
        case _ => -actionBarHeight
      }
      val show = titleYPos <= 0
      val dimAlpha = if (applyParalaxScrolling)
        math.min((0xff * (math.max(titleYPos.toFloat - actionBarHeight, 0) / actionBarHeight)).toInt, 0xff)
      else
        0

      def updateText(
        getCurrent: => CharSequence,
        storeCurrent: CharSequence => Unit,
        storedValue: => CharSequence,
        setCurrent: CharSequence => Unit): Unit = {

        val current = getCurrent
        if (current != null) {
          storeCurrent(current)
        }
        setCurrent(if (show) storedValue else null)
      }

      updateText(supportActionBar.getTitle, actionBarTitle = _, actionBarTitle, supportActionBar.setTitle)
      updateText(supportActionBar.getSubtitle, actionBarSubTitle = _, actionBarSubTitle, supportActionBar.setSubtitle)
      actionBarDimDrawable.setAlpha(dimAlpha)
      actionBarColorDrawable.setAlpha(if (show) 0xff else 0)
    }

    private def updateActionBarShadow(headerVisible: Boolean): Unit = podcastTitleContainerShadow foreach { titleShadow =>
      val titleShadowVisibility = titleShadow.getRelativeVisibleRect(getListView) match {
        case Some(rect) if headerVisible => math.max(rect.bottom - actionBarShadowOffset, 0) / titleShadow.getHeight
        case _ => 0f
      }
      titleShadow.setAlpha(titleShadowVisibility)
      actionBarShadow.foreach(_.setAlpha(1f - titleShadowVisibility))
    }

    // scroll listener

    override def onScrollStateChanged(view: AbsListView, scrollState: Int): Unit = {
      onScroll(view.getFirstVisiblePosition == 0)
      scrollListener.foreach(_.onScrollStateChanged(view, scrollState))
    }

    override def onScroll(view: AbsListView, firstVisibleItem: Int, visibleItemCount: Int, totalItemCount: Int): Unit = {
      onScroll(firstVisibleItem == 0)
      scrollListener.foreach(_.onScroll(view, firstVisibleItem, visibleItemCount, totalItemCount))
    }

    private def onScroll(headerVisible: Boolean): Unit = {
      updateImage(headerVisible)
      updateActionBar(headerVisible)
      updateActionBarShadow(headerVisible)
    }
  }

}
