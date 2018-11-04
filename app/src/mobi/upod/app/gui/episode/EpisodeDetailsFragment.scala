package mobi.upod.app.gui.episode

import android.app.Fragment
import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view._
import android.webkit.WebView
import mobi.upod.android.app.action.ActionState.ActionState
import mobi.upod.android.app.action.{Action, ActionState, AsyncAction, SimpleFragmentActions}
import mobi.upod.android.app.{FragmentStateHolder, NavigationItemSelection, SupportActionBar}
import mobi.upod.android.content.IntentHelpers.RichIntent
import mobi.upod.android.os.AsyncTask
import mobi.upod.android.view.{ChildViews, FragmentViewFinder, MenuUtil, WindowCompat}
import mobi.upod.android.widget.bottomsheet.BottomSheet
import mobi.upod.android.widget.{ActionPanel, FloatingActionButton, SlidingTitleLayout}
import mobi.upod.app.data.EpisodeListItem
import mobi.upod.app.gui.MainNavigation
import mobi.upod.app.gui.chapters.ChapterBottomSheetController
import mobi.upod.app.gui.episode.download._
import mobi.upod.app.gui.episode.library.StarEpisodeAction
import mobi.upod.app.gui.episode.news.AddEpisodeToLibraryAction
import mobi.upod.app.gui.episode.playlist._
import mobi.upod.app.services.EpisodeService
import mobi.upod.app.services.licensing.LicenseService
import mobi.upod.app.storage.EpisodeDao
import mobi.upod.app.{AppInjection, R}
import mobi.upod.media.MediaChapterTable

class EpisodeDetailsFragment
  extends Fragment
  with SimpleFragmentActions
  with EpisodeListItemView
  with EpisodeDescriptionViewController
  with ChildViews
  with FragmentViewFinder
  with FragmentStateHolder
  with SupportActionBar
  with AppInjection {

  private lazy val episodeDao = inject[EpisodeDao]
  private lazy val episodeService = inject[EpisodeService]
  private lazy val licenseService = inject[LicenseService]
  private lazy val hideActionBar = !getActivity.getResources.getBoolean(R.bool.splitScreen)
  private lazy val actionBarBackground = new ColorDrawable
  private lazy val floatingActionButton = childAs[FloatingActionButton](R.id.primaryAction)
  private lazy val actionPanel = childAs[ActionPanel](R.id.actionPanel)
  private lazy val chapterBottomSheet = childAs[BottomSheet](R.id.chapterBottomSheet)

  private var initialized = false

  private lazy val onlineEpisode = getActivity.getIntent.getExtra(NavigationItemSelection) match {
    case MainNavigation.podcasts | MainNavigation.findPodcasts => true
    case _ => false
  }

  override protected def findDescriptionView: WebView = childAs[WebView](R.id.description)

  protected def episode = episodeListItem.flatMap(e => episodeDao.find(e.id))

  private def holder = getActivity.asInstanceOf[EpisodeDetailsHolder]

  private def alternateAction(nonLibraryAction: Action, libraryAction: Action): Action = episodeListItem match {
    case Some(e) if e.library => libraryAction
    case _ => nonLibraryAction
  }

  private def markReadAction = alternateAction(
    new AddEpisodeToLibraryAction(episodeListItem) with NonLibraryRemovalAction,
    new AddEpisodeToLibraryAction(episodeListItem) with UpdateAction
  )

  protected def createActions: Map[Int, Action] = Map(
    R.id.action_add_to_library -> new AddEpisodeToLibraryAction(episodeListItem) with NonLibraryRemovalAction with UpdateAction,
    R.id.action_mark_read -> new AddEpisodeToLibraryAction(episodeListItem) with NonLibraryRemovalAction with UpdateAction,

    R.id.action_download_error -> new EpisodeDownloadErrorAction(episodeListItem),
    R.id.action_download -> new DownloadEpisodeAction(episodeListItem) with UpdateAction,
    R.id.action_stop_download -> new StopEpisodeDownloadAction(episodeListItem),
    R.id.action_add_download -> new AddEpisodeToDownloadListAction(episodeListItem) with UpdateAction,
    R.id.action_delete_download -> new DeleteEpisodeDownloadAction(episodeListItem) with UpdateAction,
    R.id.action_remove_from_download_list -> new RemoveEpisodeFromDownloadListAction(episodeListItem) with UpdateAction with DownloadListRemovalAction,

    R.id.action_cast -> new CastEpisodeAction(episodeListItem) with UpdateAction,
    R.id.action_stream -> new StreamEpisodeAction(episodeListItem) with UpdateAction,
    R.id.action_play -> new PlayEpisodeAction(episodeListItem) with UpdateAction,
    R.id.action_pause -> new PauseEpisodeAction(episodeListItem),
    R.id.action_play_next -> new PlayEpisodeNextAction(episodeListItem) with UpdateAction,
    R.id.action_add_to_playlist -> new AddEpisodeToPlaylistAction(episodeListItem) with UpdateAction,
    R.id.action_remove_from_playlist -> new RemoveEpisodeFromPlaylistAction(episodeListItem) with UpdateAction with PlaylistRemovalAction,

    R.id.action_star -> new StarEpisodeAction(episodeListItem, true) with UpdateAction,
    R.id.action_unstar -> new StarEpisodeAction(episodeListItem, false) with UpdateAction,

    R.id.action_mark_finished -> new MarkEpisodeFinishedAction(episodeListItem) with UnfinishedRemovalAction with UpdateAction,
    R.id.action_mark_unfinished -> new MarkEpisodeUnfinishedAction(episodeListItem) with UpdateAction,

    R.id.action_share -> new ShareEpisodeAction(episode),
    R.id.action_browse -> new BrowseEpisodeAction(episode),

    R.id.action_show_chapters -> ShowChaptersAction
  )

  protected lazy val view: View = childView(R.id.episodeHeader)

  private def descriptionContainer = childAs[SlidingTitleLayout](R.id.rootLayout)

  protected def invalidateMenu(): Unit = invalidateOptionsMenu()

  override protected def invalidateOptionsMenu(): Unit = {
    super.invalidateOptionsMenu()
    actionPanel.invalidateActions()
  }

  protected def optionsMenuResourceId: Int = episodeListItem match {
    case Some(e) if !e.library && onlineEpisode =>
      R.menu.online_episode_details_actions
    case Some(e) if !e.library =>
      R.menu.new_episode_details_actions
    case Some(e) if e.library =>
      R.menu.library_episode_details_actions
    case _ =>
      R.menu.empty
  }

  override def onCreateOptionsMenu(menu: Menu, inflater: MenuInflater): Unit = {
    super.onCreateOptionsMenu(menu, inflater)
    MenuUtil.filter(menu, !isActionPanelItem(_))
  }

  override protected def contextMenuResourceId: Int = optionsMenuResourceId

  override def onCreateMenu(menu: Menu, view: View): Unit = {
    super.onCreateMenu(menu, view)
    MenuUtil.filter(menu, isActionPanelItem)
  }

  private def isActionPanelItem(item: MenuItem): Boolean = {
    item.getIcon != null &&
      item.getItemId != R.id.action_download_error &&
      item.getItemId != R.id.action_share &&
      item.getItemId != R.id.action_star &&
      item.getItemId != R.id.action_unstar &&
      item.getItemId != R.id.action_show_chapters
  }

  override def onContextItemSelected(item: MenuItem): Boolean =
    super[EpisodeListItemView].onContextItemSelected(item)

  protected def showPodcastTitle: Boolean = true

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle) = {
    val layout = inflater.inflate(R.layout.episode_details_fragment, container)
    layout.childAs[SlidingTitleLayout](R.id.rootLayout).setOnScrollListener(Some(TitleScrollListener))
    layout
  }

  private def initializeIfRequired(): Unit = {
    if (!initialized) {
      initialized = true
      initDescriptionView()
      createListItemView()
      supportActionBar.setBackgroundDrawable(actionBarBackground)
      TitleScrollListener.init()
      actionPanel.listener = this
      actionPanel.primaryActionChooser = this
    }
  }

  override def setEpisode(e: EpisodeListItem): Unit = {
    initializeIfRequired()
    super.setEpisode(e)
    descriptionContainer.showFullTitle()
    asyncUpdateDescriptionView(episodeDao.find(e.id))
    loadAndUpdateChapters(e)
    actionPanel.invalidateActions()
    TitleScrollListener.invalidate()
  }

  override protected def tintViews(item: EpisodeListItem): Unit = {
    val bgColor = item.extractedOrGeneratedColors.nonLightBackground
    view.setBackgroundColor(bgColor)
    episodeTitleView.setTextColor(theme.Colors.PrimaryDarkTextColor)
    mediaProgressView.setTint(theme.Colors.PrimaryDarkTextColor)
    actionBarBackground.setColor(bgColor)
    WindowCompat.setStatusBarColor(getActivity.getWindow, bgColor.dimmed)

    val accentColor = item.extractedOrGeneratedColors.accentForNonLightBackground(theme)
    floatingActionButton.setColor(accentColor)
  }

  private trait UpdateAction extends EpisodeUpdate {
    protected def updateEpisode(episode: EpisodeListItem): Unit = {
      if (EpisodeDetailsFragment.this.state.started && episodeListItem.exists(_.id == episode.id)) {
        setEpisode(episode)
      }
    }
  }

  private trait ListRemovalAction extends AsyncAction[EpisodeListItem, EpisodeListItem] {
    protected def navIdAffected(navId: Long): Boolean

    override protected def preProcessData(context: Context, data: EpisodeListItem): Unit = {
      if (navIdAffected(holder.navigationItemId)) {
        holder.removeEpisodeFromList(data)
      }
      super.preProcessData(context, data)
    }
  }

  private trait UnfinishedRemovalAction extends ListRemovalAction {
    protected def navIdAffected(navId: Long) =
      navId == MainNavigation.newEpisodes ||
      navId == MainNavigation.library ||
      navId == MainNavigation.audioEpisodes ||
      navId == MainNavigation.videoEpisodes ||
      navId == MainNavigation.playlist
  }

  private trait NonLibraryRemovalAction extends ListRemovalAction {
    protected def navIdAffected(navId: Long) =
      navId == MainNavigation.newEpisodes || navId == MainNavigation.podcasts || navId == MainNavigation.findPodcasts
  }

  private trait PlaylistRemovalAction extends ListRemovalAction {
    protected def navIdAffected(navId: Long) = navId == MainNavigation.playlist
  }

  private trait DownloadListRemovalAction extends ListRemovalAction {
    protected def navIdAffected(navId: Long) = navId == MainNavigation.downloads
  }

  private object TitleScrollListener extends SlidingTitleLayout.OnScrollListener {
    private lazy val actionBarHeight = theme.Dimensions.ActionBarSize
    private lazy val actionBarSpacer = childView(R.id.actionBarSpacer)
    private lazy val actionBarShadow = getActivity.getWindow.getDecorView.optionalChildAs[View](R.id.actionBarShadow)
    private lazy val headerShadow = getView.optionalChildAs[View](R.id.episodeHeaderShadow)
    private lazy val actionBarShadowOffset = if (hideActionBar) actionBarHeight else 0
    private var offset = 0

    def init(): Unit = {
      if (hideActionBar) {
        actionBarSpacer.show()
      }
      invalidate()
      updateShadows(offset)
    }

    override def onTitleOffset(offset: Int): Unit = {
      this.offset = offset
      invalidate()
      updateShadows(offset)
    }

    def invalidate(): Unit = if (hideActionBar) {
      if (offset <= -actionBarHeight / 3)
        showActionBarTitle()
      else
        hideActionBarTitle()
    }

    private def showActionBarTitle(): Unit = {
      episodeListItem foreach { episode =>
        supportActionBar.setTitle(episode.title)
        supportActionBar.setSubtitle(episode.podcastInfo.title)
      }
    }

    private def hideActionBarTitle(): Unit = {
      supportActionBar.setTitle(null)
      supportActionBar.setSubtitle(null)
    }

    private def updateShadows(offset: Int): Unit = headerShadow foreach { shadow =>
      val headerShadowAlpha = if (shadow.getHeight > 0)
        math.max(shadow.getBottom + offset - actionBarShadowOffset, 0) / shadow.getHeight
      else
        1f
      shadow.setAlpha(headerShadowAlpha)
      actionBarShadow.foreach(_.setAlpha(1f - headerShadowAlpha))
    }
  }

  //
  // chapter handling
  //

  private var chapters = MediaChapterTable()
  private var chapterBottomSheetController: Option[ChapterBottomSheetController] = None

  private def loadAndUpdateChapters(episode: EpisodeListItem): Unit = {
    AsyncTask.onResult(episodeService.futureChapters(episode)) { chapters =>
      this.chapters = chapters
      onChaptersChanged(episode, chapters)
    }
  }

  private def onChaptersChanged(episode: EpisodeListItem, chapters: MediaChapterTable): Unit = {
    if (chapters.size > 1)
      recreateBottomSheetControllerIfDataChanged(episode, chapters)
    else
      destroyBottomSheetControllerIfExists()

    invalidateMenu()
  }

  private def destroyBottomSheetControllerIfExists(): Unit = {
    chapterBottomSheetController.foreach(_.destroy())
    chapterBottomSheetController = None
  }

  private def recreateBottomSheetControllerIfDataChanged(episode: EpisodeListItem, chapters: MediaChapterTable): Unit = {
    if (!chapterBottomSheetController.exists(_.episode.id == episode.id)) {
      destroyBottomSheetControllerIfExists()
      chapterBottomSheetController =
        Some(new ChapterBottomSheetController(getActivity, episode, chapters, chapterBottomSheet).create())
    }
  }

  object ShowChaptersAction extends Action {

    override def state(context: Context): ActionState =
      if (licenseService.isLicensed && chapterBottomSheetController.nonEmpty) ActionState.enabled else ActionState.gone

    override def onFired(context: Context): Unit =
      chapterBottomSheetController.foreach(_.toggleBottomSheet())
  }
}
