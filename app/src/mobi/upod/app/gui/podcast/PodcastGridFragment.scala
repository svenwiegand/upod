package mobi.upod.app.gui.podcast

import android.content.{Context, Loader}
import android.os.Bundle
import android.view.{ActionMode, View}
import android.widget.{AbsListView, AdapterView, GridView}
import mobi.upod.android.app.action._
import mobi.upod.android.app.{FragmentStateHolder, GridFragment, InitialFragmentCreation, SupportActionBar}
import mobi.upod.android.logging.Logging
import mobi.upod.android.view.DisplayUnits.Pixels
import mobi.upod.android.view.cards.CardHeaders
import mobi.upod.android.view.{ChildViews, DisplayMetrics, FragmentViewFinder}
import mobi.upod.app.data.PodcastListItem
import mobi.upod.app.gui.{AnnouncementCardHeaders, PodcastEpisodesActivity, ReloadOnEpisodeListChangedFragment}
import mobi.upod.app.services.sync.SyncService
import mobi.upod.app.storage.PodcastGridType.PodcastListType
import mobi.upod.app.storage.{ImageSize, PodcastGridType, UiPreferences}
import mobi.upod.app.{AppInjection, R}

private[podcast] abstract class PodcastGridFragment(
    navId: Long,
    emptyTextId: Int)
  extends GridFragment
  with CardHeaders with AnnouncementCardHeaders
  with ReloadOnEpisodeListChangedFragment[IndexedSeq[PodcastListItem]]
  with SupportActionBar
  with InitialFragmentCreation
  with FragmentStateHolder
  with ContextualActions
  with ImplicitFragmentContext
  with ChildViews
  with AppInjection
  with FragmentViewFinder
  with AdapterView.OnItemClickListener
  with Logging {

  private lazy val uiPreferences = inject[UiPreferences]
  private var latestGridType: PodcastListType = PodcastGridType.LargeGrid
  private var latestTitleShowState: Boolean = true
  private var lastErrorMessage: Option[CharSequence] = None

  protected def contextualMenuResourceId = R.menu.podcasts_contextual

  def adapter = getGridAdapter.asInstanceOf[PodcastListItemAdapter]

  protected def setLastError(errorMsg: CharSequence): Unit =
    lastErrorMessage = Some(errorMsg)

  protected def resetLastError(): Unit =
    lastErrorMessage = None

  override def onActivityCreated(savedInstanceState: Bundle) {
    super.onActivityCreated(savedInstanceState)

    initGrid()
    setEmptyText(getString(emptyTextId))
  }

  override protected def onInitialActivityCreation(): Unit = {
    super.onInitialActivityCreation()

    onAddHeaders()
  }

  override protected def onActivityRecreated(savedInstanceState: Bundle): Unit = {
    if (hasHeaders) {
      val gridView = getGridView
      val adapter = gridView.getAdapter
      gridView.setAdapter(null)
      onAddHeaders()
      gridView.setAdapter(adapter)
    }
    super.onActivityRecreated(savedInstanceState)
  }

  override def onStart() = {
    super.onStart()
    if (gridDisplayOptionsChanged) {
      initGrid()
      recreateAdapterIfExists()
    }
  }

  protected def gridDisplayOptionsChanged: Boolean =
    preferredGridType != latestGridType || uiPreferences.showPodcastGridTitle.get != latestTitleShowState

  private def preferredGridType: PodcastListType =
    uiPreferences.podcastGridType

  protected def initGridMetrics(gridView: GridView)(implicit displayMetrics: DisplayMetrics): Unit = {
    val gridSpacing = if (uiPreferences.showPodcastGridTitle)
      getResources.getDimensionPixelSize(R.dimen.grid_spacing) / 2
    else
      getResources.getDimensionPixelSize(R.dimen.grid_spacing)
    val gridPadding = getResources.getDimensionPixelSize(R.dimen.grid_padding)

    def initLargeGrid(): Unit = {
      gridView.setColumnWidth(150.dp.toPx)
      gridView.setNumColumns(GridView.AUTO_FIT)

      gridView.setPadding(gridPadding, gridPadding, gridPadding, gridPadding)
      gridView.setHorizontalSpacing(gridSpacing)
      gridView.setVerticalSpacing(gridSpacing)
    }

    def initTinyGrid(): Unit = {
      gridView.setColumnWidth(96.dp.toPx)
      gridView.setNumColumns(GridView.AUTO_FIT)

      gridView.setPadding(gridPadding, gridPadding, gridPadding, gridPadding)
      gridView.setHorizontalSpacing(gridSpacing)
      gridView.setVerticalSpacing(gridSpacing)
    }

    latestTitleShowState = uiPreferences.showPodcastGridTitle
    latestGridType = preferredGridType
    latestGridType match {
      case PodcastGridType.LargeGrid => initLargeGrid()
      case PodcastGridType.TinyGrid => initTinyGrid()
    }
  }

  protected def choiceMode = AbsListView.CHOICE_MODE_MULTIPLE_MODAL

  private def initGrid(): Unit = {
    val gridView = getGridView

    gridView.setSelector(R.color.transparent)
    gridView.setOnItemClickListener(this)
    gridView.setChoiceMode(choiceMode)
    gridView.setMultiChoiceModeListener(this)

    initGridMetrics(gridView)(DisplayMetrics(getActivity))
  }

  private def recreateAdapterIfExists(): Unit = {
    Option(adapter) match {
      case Some(a) =>
        setAdapter(a.items, true)
      case None =>
    }
  }

  protected def itemLayoutResource: Int = preferredGridType match {
    case PodcastGridType.LargeGrid => R.layout.podcast_grid_item
    case PodcastGridType.TinyGrid => R.layout.podcast_grid_tiny_item
  }

  protected def itemImageSize: ImageSize =
    ImageSize.grid

  protected def slowLoadingImages: Boolean = false

  protected def isGridDisplay = true

  protected def setAdapter(data: IndexedSeq[PodcastListItem], forceNewAdapter: Boolean = false) {
    lastErrorMessage match {
      case Some(msg) => setEmptyText(msg)
      case _ => setEmptyText(getString(emptyTextId))
    }
    Option(adapter) match {
      case Some(a) if !forceNewAdapter =>
        a.setItems(data)
      case _ =>
        setGridAdapter(new PodcastListItemAdapter(data, itemLayoutResource, itemImageSize, isGridDisplay, slowLoadingImages))
    }
  }

  def onLoadFinished(loader: Loader[IndexedSeq[PodcastListItem]], data: IndexedSeq[PodcastListItem]) {
    setAdapter(data)
  }

  def onLoaderReset(loader: Loader[IndexedSeq[PodcastListItem]]): Unit = if (state.started) {
    setAdapter(IndexedSeq())
    if (optionalChildAs[GridView](R.id.grid).isDefined && !getActivity.isFinishing) {
      setGridShown(false)
    }
  }

  protected override def getCurrentLoadingMessageId: Int =
    if (inject[SyncService].running) R.string.syncing else R.string.loading

  protected def translatePosition(pos: Int): Int = {
    val gridView = getGridView
    val headerFactor = gridView.getHeaderViewCount * gridView.getNumColumns
    pos - headerFactor * gridView.getHeaderViewCount
  }

  def onItemClick(parent: AdapterView[_], view: View, position: Int, id: Long): Unit = {
    PodcastEpisodesActivity.start(getActivity, navId, adapter.items(translatePosition(position)))
  }

  //
  // contextual action
  //

  protected def checkedPodcastIds: Set[Long] = getGridView.getCheckedItemIds.toSet

  protected def checkedPodcasts: IndexedSeq[PodcastListItem] = try {
    val checkedIds = checkedPodcastIds
    adapter.items.filter(podcast => checkedIds.contains(podcast.id))
  } catch {
    case error: Throwable =>
      log.crashLogError("failed to find checked Podcasts", error)
      IndexedSeq()
  }

  override def onItemCheckedStateChanged(mode: ActionMode, position: Int, id: Long, checked: Boolean) {
    mode.setTitle(getGridView.getCheckedItemCount.toString)
    super.onItemCheckedStateChanged(mode, position, id, checked)
  }

  trait BulkPodcastAdapterUpdate extends BulkPodcastUpdate {
    protected def updatePodcasts(podcasts: Traversable[PodcastListItem]) {
      adapter.update(podcasts)
      invalidateActionMode()
    }
  }

  trait ImmediateReload extends BulkPodcastAction {
    override protected def postProcessData(context: Context, result: Traversable[PodcastListItem]) = {
      reload()
    }
  }

  //
  // headers
  //

  override protected def addHeader(view: View): Unit =
    getGridView.addHeaderView(view, null, false)

  override protected def removeHeader(view: View): Unit =
    getGridView.removeHeaderView(view)

  override protected def onHeaderLayoutChanged(): Unit =
    Option(adapter).foreach(_.notifyDataSetChanged())
}
