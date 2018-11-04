package mobi.upod.app.gui.playback

import android.app.Fragment
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import android.support.v4.app.ActivityOptionsCompat
import android.view.{LayoutInflater, View, ViewGroup}
import android.widget._
import mobi.upod.android.app.action.{Action, SimpleFragmentActions}
import mobi.upod.android.app.{FragmentStateHolder, ObservableFragmentLifecycle, SupportActionBar}
import mobi.upod.android.content.IntentHelpers.RichIntent
import mobi.upod.android.graphics.Color
import mobi.upod.android.os.AsyncTask
import mobi.upod.android.view._
import mobi.upod.android.widget.FloatingActionButton
import mobi.upod.android.widget.bottomsheet.BottomSheet
import mobi.upod.app.data.{EpisodeBaseWithPlaybackInfo, EpisodeListItem}
import mobi.upod.app.gui.episode._
import mobi.upod.app.gui.episode.library.StarEpisodeAction
import mobi.upod.app.gui.{MainActivity, MainNavigation}
import mobi.upod.app.services.cast.MediaRouteService
import mobi.upod.app.{AppInjection, R}
import mobi.upod.media.MediaChapterTable

import scala.util.Try

private[playback] abstract class PlaybackFragment
  extends Fragment
  with PlaybackPanelWithSeekBar
  with FragmentStateHolder
  with ObservableFragmentLifecycle
  with SimpleFragmentActions
  with ChildViews
  with FragmentViewFinder
  with SupportActionBar
  with AppInjection {

  override protected val optionsMenuResourceId: Int = R.menu.playback_actions

  override protected def createActions = super.createActions ++ Map(
    R.id.action_show_notes -> Action(showShowNotes()),
    R.id.action_star -> new StarEpisodeAction(episode, true) with UpdateAction,
    R.id.action_unstar -> new StarEpisodeAction(episode, false) with UpdateAction,
    R.id.action_share -> new ShareEpisodeAction(episode)
  )

  private lazy val mediaRouteService = inject[MediaRouteService]

  private lazy val playlistEmptyMessageView = playbackPanel.childTextView(R.id.playlistEmptyMessage)
  private lazy val playlistButton = optionalChildAs[Button](R.id.playlistButton)
  private lazy val nextEpisodeEmptyView = optionalChildAs[View](R.id.playlistNextEmpty)
  private lazy val nextEpisodeContainer = optionalChildAs[ViewGroup](R.id.playlistNext)
  private lazy val nextEpisodeTitleView = optionalChildAs[TextView](R.id.nextEpisodeTitle)
  private lazy val chapterBottomSheet = optionalChildAs[BottomSheet](R.id.chapterBottomSheet)
  private var chapters = MediaChapterTable()
  private var chapterBottomSheetController: Option[PlaybackChapterBottomSheetController] = None

  setRetainInstance(false)

  protected def viewId: Int

  protected def requestedScreenOrientation: Int =
    ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle): View =
    inflater.inflate(viewId, null)

  override def onActivityCreated(savedInstanceState: Bundle): Unit = {
    getActivity.setRequestedOrientation(requestedScreenOrientation)
    updateActionBarBackground(Color(0))
    super.onActivityCreated(savedInstanceState)
    createPlaybackPanel()
    playlistButton.foreach(_.onClick(MainActivity.start(getActivity, MainNavigation.playlist)))
    nextEpisodeEmptyView.foreach(_.hide())
    nextEpisodeContainer.foreach(_.hide())
    chapterBottomSheet.foreach(_.setOnVisibleHeightChangedListener(ChapterBarHeightListener))
    onEpisodeChanged(getActivity.getIntent.getExtra(PlayingEpisodeExtra))
  }

  override def onStart() = {
    super.onStart()
    onActivityStart()
    createBottomSheetControllerIfRequired()
  }

  override def onStop() = {
    onActivityStop()
    destroyBottomSheetControllerIfExists()
    super.onStop()
  }

  protected def activityIsAlive: Boolean =
    getActivity != null && !getActivity.isFinishing

  override protected def onMediaInfoClick(): Unit = ()

  private def showShowNotes(): Unit = {
    val rootView = getView
    val playbackButton = childView(R.id.action_media_resume)
    val verticalOffset = playbackButton.getRelativeVisibleRect(rootView).map(_.top).getOrElse(0)
    val extras = ActivityOptionsCompat.makeScaleUpAnimation(rootView, 0, verticalOffset, rootView.getWidth, 0).toBundle
    PlaybackShowNotesActivity.start(getActivity, episode, Some(extras))
  }

  override def onEpisodeChanged(episode: Option[EpisodeBaseWithPlaybackInfo]): Unit = if (state.created) {
    super.onEpisodeChanged(episode)
    showNextEpisode()
  }

  override protected def showEpisode(episode: EpisodeListItem): Unit = {
    super.showEpisode(episode)
    updateWindowColorsForEpisode(episode)
    loadAndUpdateChapters(episode)
  }

  override def onPlaylistChanged(): Unit = {
    super.onPlaylistChanged()
    showNextEpisode()
  }

  override protected def showControls(show: Boolean): Unit = {
    super.showControls(show)
    playlistEmptyMessageView.show(!show)
    invalidateOptionsMenu()
  }

  private def updateWindowColorsForEpisode(episode: EpisodeBaseWithPlaybackInfo): Unit = {
    val bgColor = episode.extractedOrGeneratedColors.nonLightBackground
    val bgColorDimmed = bgColor.dimmed
    updateActionBarBackground(bgColorDimmed)
    WindowCompat.setStatusBarColor(getActivity.getWindow, bgColorDimmed)
  }

  protected def updateActionBarBackground(color: Color): Unit = ()

  protected def updatePlaybackControlBackground(color: Color): Unit = {
    playbackControl.setBackgroundColor(color)
  }

  private def showNextEpisode(): Unit = {
    AsyncTask.execute(playbackService.findSecondPlaylistEpisode) { episode =>
      if (getActivity != null && !getActivity.isFinishing) {
        showNextEpisode(episode)
      }
    }
  }

  private def showNextEpisode(episode: Option[EpisodeListItem]) {
    nextEpisodeContainer.foreach(_.show(episode.isDefined))
    nextEpisodeEmptyView.foreach(_.show(episode.isEmpty))
    episode.foreach { e =>
      nextEpisodeTitleView.foreach(_.setText(e.title))
      nextEpisodeContainer.foreach(_.onClick(showEpisodeDetails(e)))
    }
  }

  private def showEpisodeDetails(episode: EpisodeListItem) {
    EpisodeDetailsActivity.start(getActivity, episode, MainNavigation.playlist, MainNavigation.viewModeIdEpisodes, _ => ())
  }

  protected def isRemotePlayback: Boolean = mediaRouteService.currentDevice.isDefined

  override protected def tintViews(episode: EpisodeListItem): Unit = {
    super.tintViews(episode)

    val bgColor = episode.extractedOrGeneratedColors.nonLightBackground
    val bgColorDimmed = bgColor.dimmed
    val accentColor = episode.extractedOrGeneratedColors.accentForNonLightBackground(theme)
    updatePlaybackControlBackground(bgColor)
    updateActionBarBackground(bgColorDimmed)
    WindowCompat.setStatusBarColor(getActivity.getWindow, bgColorDimmed)
    Tintable.tintOrIgnore(seekBar, theme.Colors.White)
    mediaPlayButton.asInstanceOf[FloatingActionButton].setColor(accentColor)
    mediaPauseButton.asInstanceOf[FloatingActionButton].setColor(accentColor)
  }

  protected def isLandscape: Boolean = {
    // An IllegalStateException might be thrown when querying the orientation while the fragment isn't attached
    // to an activity
    Try(getResources.getConfiguration.orientation == Configuration.ORIENTATION_LANDSCAPE).getOrElse(
      requestedScreenOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
  }

  override def invalidateActionButtons(): Unit = {
    super.invalidateActionButtons()
    invalidateOptionsMenu()
  }

  //
  // chapter handling
  //

  private def loadAndUpdateChapters(episode: EpisodeListItem): Unit = {
    AsyncTask.onResult(episodeService.futureChapters(episode)) { chapters =>
      this.chapters = chapters
      onChaptersChanged(episode, chapters)
    }
  }

  protected def onChaptersChanged(episode: EpisodeListItem, chapters: MediaChapterTable): Unit = chapterBottomSheet foreach { bottomSheet =>
    if (chapters.size > 1)
      recreateBottomSheetControllerIfDataChanged(episode, chapters, bottomSheet)
    else
      destroyBottomSheetControllerIfExists()
  }

  private def createBottomSheetControllerIfRequired(): Unit = {
    (episode, chapterBottomSheet) match {
      case (Some(e), Some(bottomSheet)) if chapters.size > 1 =>
        recreateBottomSheetControllerIfDataChanged(e, chapters, bottomSheet)
      case _ =>
        destroyBottomSheetControllerIfExists()
    }
  }

  private def destroyBottomSheetControllerIfExists(): Unit = {
    chapterBottomSheetController.foreach(_.destroy())
    chapterBottomSheetController = None
  }

  private def recreateBottomSheetControllerIfDataChanged(
      episode: EpisodeListItem,
      chapters: MediaChapterTable,
      bottomSheet: BottomSheet): Unit = {

    if (!chapterBottomSheetController.exists(_.episode.id == episode.id)) {
      destroyBottomSheetControllerIfExists()
      chapterBottomSheetController = {
        val controller = new PlaybackChapterBottomSheetController(getActivity, episode, chapters, bottomSheet)
        controller.create()
        Some(controller)
      }
    }
  }

  private object ChapterBarHeightListener extends BottomSheet.OnVisibleHeightChangedListener {

    private def setPlaybackControlBottomPadding(padding: Int): Unit =
      playbackControl.setPadding(playbackControl.getPaddingLeft, playbackControl.getPaddingTop, playbackControl.getPaddingRight, padding)

    override def onBottomSheetHeightChanged(persistentHeight: Int, height: Int): Unit =
      setPlaybackControlBottomPadding(math.min(persistentHeight, height))
  }

  //
  // episode update actions
  //

  private trait UpdateAction extends EpisodeUpdate {
    protected def updateEpisode(episode: EpisodeListItem): Unit = {
      if (this.episode.exists(_.id == episode.id)) {
        PlaybackFragment.this.episode = Some(episode)
        invalidateOptionsMenu()
      }
    }
  }
}
