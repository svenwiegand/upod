package mobi.upod.app.gui.episode

import java.net.URL

import android.content.Context
import android.view.{Menu, MenuItem, View}
import com.escalatesoft.subcut.inject.Injectable
import mobi.upod.android.app.ActivityLifecycleListener
import mobi.upod.android.app.action.ContextMenuActions
import mobi.upod.android.graphics.Color
import mobi.upod.android.view.Helpers.RichView
import mobi.upod.android.widget.{PrimaryActionChooser, TintableProgressBar}
import mobi.upod.app.R
import mobi.upod.app.data.{EpisodeBaseWithDownloadInfo, EpisodeBaseWithPlaybackInfo, EpisodeListItem}
import mobi.upod.app.gui._
import mobi.upod.app.services.cast.{MediaRouteDevice, MediaRouteListener, MediaRouteService}
import mobi.upod.app.services.download.{DownloadListener, DownloadService}
import mobi.upod.app.services.playback.{PlaybackListener, PlaybackService}
import mobi.upod.app.storage.{ImageSize, PlaybackPreferences, UiPreferences}
import mobi.upod.util.DateTimeUtils.RichDateTime
import mobi.upod.util.Permille._

private[episode] trait EpisodeListItemView
  extends ActivityLifecycleListener
  with PrimaryActionChooser
  with ContextMenuActions
  with DownloadListener
  with PlaybackListener
  with MediaRouteListener
  with Injectable {

  protected lazy val coverartLoader = inject[CoverartLoader]
  protected lazy val downloadService = inject[DownloadService]
  protected lazy val playbackService = inject[PlaybackService]
  protected lazy val mediaRouteService = inject[MediaRouteService]
  protected lazy val playbackPreferences = inject[PlaybackPreferences]
  protected lazy val uiPreferences = inject[UiPreferences]

  protected def view: View
  private var _episode: Option[EpisodeListItem] = None
  protected var imageUrl: Option[URL] = None
  protected lazy val theme = new Theme(view.getContext)
  protected lazy val imageView = view.childImageView(R.id.podcastImage)
  protected lazy val starIndicator = view.childView(R.id.starIndicator)
  protected lazy val mediaTypeIndicator = view.childImageView(R.id.mediaTypeIndicator)
  protected lazy val playlistIndicator = view.childView(R.id.playlistIndicator)
  protected lazy val downloadListIndicator = view.childView(R.id.downloadListIndicator)
  protected lazy val podcastTitleView = view.childTextView(R.id.podcastTitle)
  protected lazy val episodeTitleView = view.childTextView(R.id.episodeTitle)
  protected lazy val mediaProgressView = view.childAs[TintableProgressBar](R.id.mediaProgress)
  protected lazy val timestampView = view.childTextView(R.id.timestamp)
  protected lazy val mediaTimeView = view.childTextView(R.id.duration)
  protected lazy val coverartPlaceholderDrawable = new CoverartPlaceholderDrawable with CoverartLoaderFallbackDrawable

  implicit def context: Context

  def episodeListItem = _episode

  protected def createListItemView(): Unit = {
    downloadService.addWeakListener(this, false)
    playbackService.addWeakListener(this, false)
    mediaRouteService.addWeakListener(this, false)
  }

  protected def setEpisode(item: EpisodeListItem) {
    _episode = Some(item)
    updatePodcastTitle(item)
    updateEpisodeTitle(item)
    updateImage(item)
    updateIndicators(item)
    updateMediaProgress(item)
    updateTimestamp(item)
    updateMediaTime(item)
    invalidateMenu()
    tintViews(item)
  }

  protected def invalidateMenu(): Unit

  protected def isDownloading: Boolean =
    downloadService.downloadingEpisode.map(_.id) == _episode.map(_.id)

  protected def isInImmediateDownloadQueue: Boolean = _episode exists { e =>
    downloadService.immediateDownloadQueue.exists(_.id == e.id)
  }

  protected def isPlaying: Boolean =
    playbackService.playingEpisode.map(_.id) == _episode.map(_.id)

  protected def itemKeyColor: Color =
    _episode.flatMap(_.extractedOrGeneratedColors.key).getOrElse(theme.Colors.Primary)

  protected def tintViews(item: EpisodeListItem): Unit =
    mediaProgressView.setTint(itemKeyColor)

  private def updateImage(item: EpisodeListItem) {
    if (imageUrl == None || item.podcastInfo.imageUrl != imageUrl) { // prevent flickering
      coverartPlaceholderDrawable.set(item.podcastInfo.title, item.extractedOrGeneratedColors)
      coverartLoader.displayImage(imageView, ImageSize.list, item.podcastInfo.imageUrl, Some(coverartPlaceholderDrawable))
      imageUrl = item.podcastInfo.imageUrl
    }
  }

  private def updateIndicators(item: EpisodeListItem) {

    def showAsFinished(finished: Boolean): Unit = {
      val alpha = if (finished) 0.5f else 1.0f
      podcastTitleView.setAlpha(alpha)
      episodeTitleView.setAlpha(alpha)
      imageView.setAlpha(alpha)
      mediaProgressView.setAlpha(alpha)
      timestampView.setAlpha(alpha)
      mediaTimeView.setAlpha(alpha)
    }

    starIndicator.show(item.starred)
    mediaTypeIndicator.setImageResource(if (item.isVideo) R.drawable.ic_episode_indicator_video else R.drawable.ic_episode_indicator_audio)
    playlistIndicator.show(item.playbackInfo.listPosition.isDefined)
    downloadListIndicator.show(item.downloadInfo.listPosition.isDefined)

    val textAppearance = if (item.isNew) R.style.TextAppearance_List_PrimaryText_Emphasized else R.style.TextAppearance_List_PrimaryText
    episodeTitleView.setTextAppearance(context, textAppearance)
    showAsFinished(item.playbackInfo.finished)
  }

  protected def showPodcastTitle: Boolean
  
  private def updatePodcastTitle(item: EpisodeListItem) {
    podcastTitleView.setText(item.podcastInfo.title)
    podcastTitleView.show(showPodcastTitle && item.podcastInfo.title.nonEmpty)
  }

  private def updateEpisodeTitle(item: EpisodeListItem) {
    episodeTitleView.setText(item.title)
    episodeTitleView.show(!item.title.isEmpty)
  }

  private def updateMediaProgress(item: EpisodeListItem) {
    val indeterminate =
      (isInImmediateDownloadQueue && !isDownloading) ||
      (
        isDownloading &&
        item.playbackInfo.playbackPosition <= 0 &&
        (item.downloadInfo.fetchedBytes <= 0 || item.media.length <= 0)
      )

    mediaProgressView.setIndeterminate(indeterminate)
    if (!indeterminate) {
      mediaProgressView.setMax(PermilleMax)
      if (item.media.length > 0)
        mediaProgressView.setSecondaryProgress(item.downloadInfo.fetchedBytes.permille(item.media.length).toInt)
      else if (item.downloadInfo.fetchedBytes > 0)
        mediaProgressView.setSecondaryProgress(500)
      else
        mediaProgressView.setSecondaryProgress(0)
      mediaProgressView.setProgress(item.playbackInfo.playbackPosition.permille(item.media.duration).toInt)
    }
  }

  private def updateTimestamp(item: EpisodeListItem) {
    timestampView.setText(item.published.formatRelativeDate(view.getContext))
  }

  private def updateMediaTime(item: EpisodeListItem): Unit =
    mediaTimeView.setText(item.formattedPosition(playbackPreferences.mediaTimeFormat, false))

  //
  // download listener
  //

  protected def onEpisodeDownloadInfoUpdate(episode: EpisodeListItem, stateUpdate: Boolean): Unit = ()

  private def updateEpisodeDownloadInfo(episode: EpisodeBaseWithDownloadInfo, stateUpdate: Boolean) {
    if (_episode.map(_.id) == Some(episode.id)) {
      _episode = _episode.map(e => e.copy(
        media = e.media.copy(length = episode.media.length), downloadInfo = episode.downloadInfo))
      _episode.foreach { e =>
        updateMediaProgress(e)
        if (stateUpdate) {
          invalidateMenu()
        }
        onEpisodeDownloadInfoUpdate(e, stateUpdate)
      }
    }
  }

  override def onDownloadStarted(episode: EpisodeBaseWithDownloadInfo) {
    updateEpisodeDownloadInfo(episode, true)
  }

  override def onDownloadProgress(episode: EpisodeBaseWithDownloadInfo, bytesPerSecond: Int, remainingMillis: Option[Long]) {
    updateEpisodeDownloadInfo(episode, false)
  }

  override def onDownloadStopped(episode: EpisodeBaseWithDownloadInfo) {
    updateEpisodeDownloadInfo(episode, true)
  }

  //
  // playback listener
  //

  protected def onEpisodePlaybackInfoUpdate(episode: EpisodeListItem, stateUpdate: Boolean): Unit = ()

  private def updateEpisodePlaybackInfo(episode: EpisodeBaseWithPlaybackInfo, stateUpdate: Boolean) {
    if (_episode.map(_.id) == Some(episode.id)) {
      _episode = _episode.map(e => e.copy(
        media = e.media.copy(duration = episode.media.duration), playbackInfo = episode.playbackInfo))
      _episode.foreach { e =>
        updateMediaTime(e)
        updateMediaProgress(e)
        if (stateUpdate) {
          invalidateMenu()
        }
        onEpisodePlaybackInfoUpdate(e, stateUpdate)
      }
    }
  }

  override def onPlaybackStarted(episode: EpisodeBaseWithPlaybackInfo) {
    invalidateMenu()
  }

  override def onPlaybackPaused(episode: EpisodeBaseWithPlaybackInfo) {
    invalidateMenu()
  }

  override def onPlaybackStopped() {
    invalidateMenu()
  }

  //
  // media route listener
  //

  override def onMediaRouteDeviceDisconnected(device: MediaRouteDevice): Unit =
    invalidateMenu()

  override def onMediaRouteDeviceConnected(device: MediaRouteDevice): Unit =
    invalidateMenu()

  override def onPlaybackPositionChanged(episode: EpisodeBaseWithPlaybackInfo): Unit = {
    updateEpisodePlaybackInfo(episode, false)
  }

  //
  // activity lifecycle listener
  //

  override def onActivityStart() {
    downloadService.addWeakListener(this)
    playbackService.addWeakListener(this)
    mediaRouteService.addWeakListener(this)
    invalidateMenu()
  }

  override def onActivityStop() {
    downloadService.removeListener(this)
    playbackService.removeListener(this)
    mediaRouteService.removeListener(this)
  }

  //
  // primary action chooser
  //


  override def choosePrimaryAction(menu: Menu): Option[MenuItem] = episodeListItem flatMap { episode =>
    import mobi.upod.app.storage.PrimaryDownloadAction._

    val primaryDownloadAction = uiPreferences.primaryDownloadAction.get
    val addForDownloadableNew = uiPreferences.useAddActionForDownloadableNewEpisodes
    val addForDownloadedNew = uiPreferences.useAddActionForDownloadedNewEpisodes

    def shouldBePrimary(item: MenuItem): Boolean = item.getItemId match {
      case id if id == R.id.action_download_error =>
        true
      case id if id == R.id.action_stop_download =>
        true
      case id if id == R.id.action_download =>
        primaryDownloadAction == Download ||
          (episode.downloadInfo.listPosition.isDefined && primaryDownloadAction == AddToDownloadQueue) ||
          (episode.playbackInfo.listPosition.isDefined && primaryDownloadAction == AddToPlaylist)
      case id if id == R.id.action_add_download =>
        primaryDownloadAction == AddToDownloadQueue

      case id if id == R.id.action_add_to_library || id == R.id.action_mark_read =>
        if (episode.downloadInfo.complete) addForDownloadedNew else addForDownloadableNew

      case id if id == R.id.action_pause =>
        true
      case id if id == R.id.action_cast =>
        true
      case id if id == R.id.action_stream =>
        primaryDownloadAction == Stream
      case id if id == R.id.action_play =>
        true
      case id if id == R.id.action_add_to_playlist =>
        primaryDownloadAction == AddToPlaylist

      case id if id == R.id.action_mark_unfinished =>
        true
      case _ =>
        false
    }

    def choose(index: Int): Option[MenuItem] = {
      val item = menu.getItem(index)
      if (item.isEnabled && item.isVisible && item.getIcon != null && shouldBePrimary(item))
        Some(item)
      else if (index < (menu.size() - 1))
        choose(index + 1)
      else
        None
    }

    if (menu.size > 0)
      choose(0)
    else
      None
  }
}
