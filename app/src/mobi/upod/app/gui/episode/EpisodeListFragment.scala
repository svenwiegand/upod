package mobi.upod.app.gui.episode

import android.content.{Context, Intent, Loader}
import android.os.Bundle
import android.view._
import android.widget.AbsListView.MultiChoiceModeListener
import android.widget.AdapterView.OnItemClickListener
import android.widget.{AbsListView, AdapterView, ListView}
import mobi.upod.android.app._
import mobi.upod.android.app.action.{Action, AsyncActionHook, ContextualActions, ImplicitFragmentContext}
import mobi.upod.android.content.AsyncCursorLoader
import mobi.upod.android.content.Theme._
import mobi.upod.android.view.cards.CardHeaders
import mobi.upod.android.view.{ChildViews, FragmentViewFinder}
import mobi.upod.app.data.EpisodeListItem
import mobi.upod.app.gui.{AnnouncementCardHeaders, SyncOnPull}
import mobi.upod.app.services.sync.SyncService
import mobi.upod.app.storage.{EpisodeDao, InternalAppPreferences}
import mobi.upod.app.{AppInjection, R}
import mobi.upod.util.Collections.Index
import mobi.upod.util.Cursor

import scala.util.Try

abstract class EpisodeListFragment
  extends ListFragment
  with CardHeaders with AnnouncementCardHeaders
  with SimpleReloadableFragment[IndexedSeq[EpisodeListItem]]
  with SyncOnPull
  with InitialFragmentCreation
  with FragmentStateHolder
  with ContextualActions
  with ImplicitFragmentContext
  with ChildViews
  with FragmentViewFinder
  with MultiChoiceModeListener
  with OnItemClickListener
  with ObservableFragmentLifecycle
  with AppInjection {

  private lazy val episodeDao = inject[EpisodeDao]
  protected lazy val internalAppPreferences = inject[InternalAppPreferences]

  protected def holder = getActivity.asInstanceOf[EpisodeListHolder]

  private var _adapter: Option[EpisodeAdapter] = None
  protected def optionalAdapter: Option[EpisodeAdapter] = _adapter
  protected def adapter: EpisodeAdapter = _adapter.get
  protected val dismissController = new EpisodeDismissController(adapter, getListView, holder)

  protected def enableActions = holder.enableEpisodeActions
  protected def createActions: Map[Int, Action] = Map()

  protected val emptyTextId: Int
  private var lastErrorMessage: Option[CharSequence] = None

  protected def listItemBackgroundDrawable = getActivity.getThemeResource(R.attr.activatedListItemBackground)

  protected trait DismissActionInfo {
    val dismissController: Option[EpisodeDismissController] = Some(EpisodeListFragment.this.dismissController)
    def listView: ListView = getListView
  }

  protected def setLastError(errorMsg: CharSequence): Unit =
    lastErrorMessage = Some(errorMsg)

  protected def resetLastError(): Unit =
    lastErrorMessage = None

  setRetainInstance(true)

  override def onActivityCreated(savedInstanceState: Bundle) {
    super.onActivityCreated(savedInstanceState)

    val listView = getListView
    listView.setOnItemClickListener(this)
    if (holder.enableEpisodeActions)
      listView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE_MODAL)
    else if (holder.checkClickedEpisode)
      listView.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE)

    listView.setMultiChoiceModeListener(this)
    listView.setDivider(null)
    setEmptyText(getString(emptyTextId))
  }

  override protected def onInitialActivityCreation() {
    val listView = getListView
    onAddHeaders()
    onAddFooters(listView)
    prepareListForLoading()
    getLoaderManager.initLoader(0, null, this)
  }

  override protected def onActivityRecreated(savedInstanceState: Bundle) {
    if (hasHeaders || hasFooters) {
      val listView = getListView
      val adapter = listView.getAdapter
      listView.setAdapter(null)
      onAddHeaders()
      onAddFooters(listView)
      listView.setAdapter(adapter)
    }
    super.onActivityRecreated(savedInstanceState)
  }

  //
  // header and footer handling
  //

  override protected def allowCardHeaders: Boolean =
    holder.enableEpisodeActions

  override protected def addHeader(view: View): Unit =
    getListView.addHeaderView(view, null, false)

  override protected def removeHeader(view: View): Unit =
    getListView.removeHeaderView(view)

  override protected def onHeaderLayoutChanged(): Unit =
    ()// listview handles header layout change well -- so nothing to do for us

  protected def hasFooters = false

  protected def onAddFooters(listView: ListView): Unit = {}

  //
  // loading content
  //

  protected def orderedAscending: Boolean = false

  protected def loadEpisodes(dao: EpisodeDao): Cursor[EpisodeListItem]

  protected def ViewHolderConfiguration(
      showPodcastTitle: Boolean,
      podcastColoredProgressBar: Boolean,
      enableDragHandle: Boolean,
      episodeOrderControl: Option[EpisodeOrderControl] = None) = {
    EpisodeListItemViewHolderConfiguration(
      listItemBackgroundDrawable,
      showPodcastTitle,
      podcastColoredProgressBar,
      enableActions,
      enableDragHandle,
      () => orderedAscending,
      updateAdapterEpisode,
      reload,
      episodeOrderControl,
      Some(dismissController),
      this
    )
  }

  protected def createAdapter(data: IndexedSeq[EpisodeListItem]): EpisodeAdapter

  private def createAndPrepareAdapter(data: IndexedSeq[EpisodeListItem]): EpisodeAdapter = {
    val a = createAdapter(data)
    a.forceNotEmpty = Some(Unit => hasHeaders)
    a
  }

  private def setItems(data: IndexedSeq[EpisodeListItem]): Unit = {
    lastErrorMessage match {
      case Some(msg) => setEmptyText(msg)
      case _ => setEmptyText(getString(emptyTextId))
    }
    _adapter match {
      case Some(a) =>
        a.setItems(data)
      case _ =>
        _adapter = Some(createAndPrepareAdapter(data))
    }
    if (getListView.getAdapter == null) {
      setListAdapter(_adapter.get)
    }
    setListShown(true)
    getActivity.invalidateOptionsMenu()
    selectCurrentEpisodeIfApplicable()
  }

  private def clearItems(): Unit = {
    resetLastError()
    _adapter match {
      case Some(a) =>
        if (!a.isEmpty) {
          a.setItems(IndexedSeq())
          getActivity.invalidateOptionsMenu()
          selectCurrentEpisodeIfApplicable()
        }
      case _ =>
        _adapter = Some(createAndPrepareAdapter(IndexedSeq()))
    }
  }

  def onCreateLoader(id: Int, args: Bundle): Loader[IndexedSeq[EpisodeListItem]] =
    AsyncCursorLoader(getActivity, if (state.started) loadEpisodes(episodeDao) else Cursor.empty)

  def onLoadFinished(loader: Loader[IndexedSeq[EpisodeListItem]], data: IndexedSeq[EpisodeListItem]): Unit = if (state.started) {
    setItems(data)
  }

  def onLoaderReset(loader: Loader[IndexedSeq[EpisodeListItem]]) {
    prepareListForLoading()
  }

  protected def prepareListForLoading() {
    clearItems()
    Try(setListShown(false))
  }

  protected override def getCurrentLoadingMessageId: Int =
    if (inject[SyncService].running) R.string.syncing else R.string.loading

  private def selectCurrentEpisodeIfApplicable() {
    if (holder.checkClickedEpisode) {
      val position = holder.checkedEpisode.flatMap(adapter.getEpisodePosition)
      position.foreach { pos =>
        getListView.setItemChecked(pos, true)
        getListView.setSelection(pos)
      }
    }
  }

  protected def checkedEpisodeIds: Set[Long] = getListView.getCheckedItemIds.toSet

  protected def checkedEpisodes: IndexedSeq[EpisodeListItem] = {
    val checkedIds = checkedEpisodeIds
    adapter.episodes.filter(episode => checkedIds.contains(episode.id))
  }

  //
  // contextual actions
  //

  override def onItemCheckedStateChanged(mode: ActionMode, position: Int, id: Long, checked: Boolean) {
    mode.setTitle(getListView.getCheckedItemCount.toString)
    super.onItemCheckedStateChanged(mode, position, id, checked)
  }

  protected def uncheckAllEpisodes(): Unit = {
    getListView.clearChoices()
    destroyActionMode()
  }

  protected def navigationItemId: Long

  protected def viewModeId: Int

  def onItemClick(parent: AdapterView[_], view: View, position: Int, id: Long) {
    adapter.getEpisode(position - getListView.getHeaderViewsCount).foreach(openEpisode)
    if (holder.checkClickedEpisode) {
      getListView.setSelection(position)
      getListView.setItemChecked(position, true)
    }
  }

  protected def prepareEpisodeActivityIntent(intent: Intent) {
    // do nothing by default
  }

  private def openEpisode(episode: EpisodeListItem) {
    holder.openEpisode(episode, navigationItemId, viewModeId, prepareEpisodeActivityIntent)
  }

  //
  // episode adapter updates
  //

  def updateAdapterEpisode(episode: EpisodeListItem, notifyChanged: Boolean) {
    adapter.update(Traversable(episode), notifyChanged)
    invalidateActionMode()
  }

  def removeEpisode(episode: EpisodeListItem): Option[EpisodeListItem] = {
    val index = adapter.episodes.indexWhere(_.id == episode.id).validIndex
    adapter.getEpisodePosition(episode) foreach { pos =>
      getListView.setItemChecked(pos, false)
    }
    val nextEpisode: Option[EpisodeListItem] = index.flatMap { index =>
      if (index < (adapter.episodes.size - 1))
        Some(adapter.episodes(index + 1))
      else if (index > 0)
        Some(adapter.episodes(index - 1))
      else
        None
    }

    nextEpisode foreach { e =>
      adapter.getEpisodePosition(e) foreach { pos =>
        getListView.setItemChecked(pos, true)
      }

      // we only trigger the dismissController when there is a nextEpisode, because otherwise the activity will finish
      dismissController.dismiss(episode.id)
    }

    nextEpisode
  }

  trait BulkEpisodeAdapterUpdate extends BulkEpisodeUpdate {
    protected def updateEpisodes(episodes: Traversable[EpisodeListItem]): Unit = if (EpisodeListFragment.this.state.started) {
      adapter.update(episodes)
      invalidateActionMode()
    }
  }

  trait BulkEpisodeNopUpdate extends BulkEpisodeUpdate {
    protected def updateEpisodes(episodes: Traversable[EpisodeListItem]): Unit = {}
  }

  trait ActionReload extends AsyncActionHook {

    override protected def postProcess(context: Context): Unit = {
      super.postProcess(context)
      requestReload()
    }
  }
}
