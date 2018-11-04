package mobi.upod.app.services.playback

import android.app.Activity
import android.content.Context
import android.graphics.SurfaceTexture
import com.escalatesoft.subcut.inject.{BindingModule, Injectable}
import mobi.upod.android.app.BoundServiceConnection
import mobi.upod.android.logging.Logging
import mobi.upod.android.os.{AsyncExecution, AsyncObservable}
import mobi.upod.app.App
import mobi.upod.app.data._
import mobi.upod.app.gui.playback.{PlaybackActivity, PlaybackErrorActivity}
import mobi.upod.app.services.EpisodeService
import mobi.upod.app.services.cast.{MediaRouteDevice, MediaRouteListener, MediaRouteService}
import mobi.upod.app.services.download.DownloadService
import mobi.upod.app.services.net.ConnectionStateRetriever
import mobi.upod.app.services.playback.player.AudioFxAvailability
import mobi.upod.app.services.sync.SyncService
import mobi.upod.app.storage._
import mobi.upod.media.{MediaChapter, MediaChapterTable}

import scala.util.Try

class PlaybackService(implicit val bindingModule: BindingModule)
  extends BoundServiceConnection[PlaybackController]
  with AsyncObservable[PlaybackListener]
  with AsyncExecution
  with PlaybackListener
  with MediaRouteListener
  with Injectable
  with Logging {

  private val app = inject[App]
  private lazy val episodeDao = inject[EpisodeDao]
  private lazy val episodeService = inject[EpisodeService]
  private lazy val syncService = inject[SyncService]
  private lazy val downloadService = inject[DownloadService]
  private lazy val mediaRouteService = inject[MediaRouteService]
  private lazy val connectionService = inject[ConnectionStateRetriever]
  private lazy val downloadPreferences = inject[DownloadPreferences]
  private lazy val playbackPreferences = inject[PlaybackPreferences]
  private lazy val storagePreferences = inject[StoragePreferences]

  protected val serviceContext = app
  protected def serviceBinder = PlaybackServiceImpl

  private var asyncInitFinished = false
  private var _playingOrNextEpisodeId: Option[Long] = None

  //
  // constructor
  //

  asyncUpdatePlayingOrNextEpisodeId()
  mediaRouteService.addWeakListener(this)

  def playingOrNextEpisodeId: Option[Long] = _playingOrNextEpisodeId

  def loadPlayingOrNextEpisode: Option[EpisodeListItem] =
    playingOrNextEpisodeId.flatMap(episodeDao.findListItemById)

  def forPlayingOrNextEpisode(block: Option[EpisodeListItem] => Unit): Unit =
    async(loadPlayingOrNextEpisode)(block)

  def forDefinedPlayingOrNextEpisode(block: EpisodeListItem => Unit): Unit =
    forPlayingOrNextEpisode(_.foreach(block))

  def findSecondPlaylistEpisode: Option[EpisodeListItem] =
    episodeDao.findSecondPlaylistEpisode

  def findFirstPlayablePlaylistEpisodeId: Option[Long] =
    playablePlaylistEntry(episodeDao.findFirstPlaylistEpisodeId, episodeDao.findFirstFullyDownloadedPlaylistEpisodeId)

  private def playablePlaylistEntry[A](whenStreamingAllowed: => A, whenStreamingForbidden: => A): A = {
    import mobi.upod.app.services.net.ConnectionState._
    import mobi.upod.app.storage.NotDownloadedEpisodesPlaybackStrategy._

    (playbackPreferences.notDownloadedEpisodesPlaybackStrategy.get, connectionService.getConnectionState) match {
      case (StreamOnWifi, Full) => whenStreamingAllowed
      case (Stream, state) if state != Unconnected => whenStreamingAllowed
      case _ => whenStreamingForbidden
    }
  }

  //
  // playlist handling
  //

  private def findFirstPlaylistEpisodeId: Option[Long] =
    episodeDao.findFirstPlaylistEpisodeId

  private def asyncUpdatePlayingOrNextEpisodeId(): Unit = {
    async(findFirstPlaylistEpisodeId)(updatePlayingOrNextEpisodeId)
  }

  private def updatePlayingOrNextEpisodeId(): Unit = {
    updatePlayingOrNextEpisodeId(findFirstPlaylistEpisodeId)
  }

  private def updatePlayingOrNextEpisodeId(episodeId: Option[Long]): Unit = {
    if (!asyncInitFinished || _playingOrNextEpisodeId != episodeId) {
      asyncInitFinished = true
      _playingOrNextEpisodeId = episodeId
      fireEpisodeIdChanged(episodeId)
    }
  }

  private[services] def playlistChanged(): Unit = {
    updatePlayingOrNextEpisodeId()
    firePlaylistChanged()
  }

  private def firePlaylistChanged(): Unit = {
    fire(_.onPlaylistChanged())
  }

  private def updatePlaylist(update: => Unit) {
    episodeDao.inTransaction(update)
    episodeService.fireEpisodeCountChanged()
    syncService.playlistUpdated()
    updatePlayingOrNextEpisodeId()
    firePlaylistChanged()
  }

  private def asyncUpdatePlaylist(update: => Unit) {
    async(updatePlaylist(update))
  }

  private def addToDownloadListIfApplicable(ids: Traversable[Long]) {
    if (downloadPreferences.shouldAutoAddPlaylistEpisodes) {
      downloadService.addDownloads(ids)
    }
  }

  def addEpisodes(ids: Traversable[Long]) {
    updatePlaylist {
      episodeDao.addToLibrary(ids)
      episodeDao.addToPlaylist(ids)
      addToDownloadListIfApplicable(ids)
    }
  }

  def addRandomEpisode() {
    episodeDao.findRandomNonPlaylistLibraryEpisodeIdPreferringDownloaded.foreach { id =>
      updatePlaylist {
        addEpisodes(Traversable(id))
      }
    }
  }

  def playNext(episode: EpisodeListItem) {
    updatePlaylist {
      episodeDao.addToLibrary(Seq(episode.id))
      episodeDao.playNext(episode.episodeId, !isIdle)
      addToDownloadListIfApplicable(Traversable(episode.id))
    }
  }

  def canRemoveAtLeastOneOf(episodes: Traversable[EpisodeBaseWithPlaybackInfo]): Boolean =
    filterRemovable(episodes).nonEmpty

  def filterRemovable[A <: EpisodeBaseWithPlaybackInfo](episodes: Traversable[A]): Traversable[A] = {
    val playingId = playingEpisode.map(_.id).getOrElse(0)
    episodes.filter(e => e.playbackInfo.listPosition.isDefined && e.id != playingId)
  }

  def removeEpisodes[A <: EpisodeBaseWithPlaybackInfo](episodes: Traversable[A]): Traversable[A] = {
    val removable = filterRemovable(episodes)
    val removableIds = removable.map(_.id)
    updatePlaylist(episodeDao.removeFromPlaylist(removableIds))
    removable
  }

  def removeEpisode(episode: EpisodeBaseWithPlaybackInfo): Boolean =
    removeEpisodes(Traversable(episode)).nonEmpty

  def asyncUpdatePlaylistPositions(episodes: IndexedSeq[EpisodeListItem]) {
    asyncUpdatePlaylist(episodeDao.updatePlaylist(episodes.map(_.episodeId)))
  }

  private[playback] def insertEpisodeAtStart(episode: EpisodeId): Unit = {
    updatePlaylist {
      episodeDao.addToLibraryByIds(Seq(episode))
      episodeDao.insertAtStartOfPlaylist(episode)
    }
  }

  //
  // position handling
  //

  def canMarkFinished(episode: EpisodeBaseWithPlaybackInfo): Boolean =
    !episode.playbackInfo.finished

  def canMarkFinishedAtLeastOneOf(episodes: Traversable[EpisodeBaseWithPlaybackInfo]): Boolean =
    filterCanMarkFinished(episodes).nonEmpty

  def filterCanMarkFinished[A <: EpisodeBaseWithPlaybackInfo](episodes: Traversable[A]): Traversable[A] =
    episodes.filter(canMarkFinished)

  def markEpisodesFinished[A <: EpisodeBaseWithPlaybackInfo](episodes: Traversable[A]): Traversable[A] = {
    val markable = filterCanMarkFinished(episodes)
    updatePlaylist(episodeDao.updatePlaybackFinished(markable.map(_.id)))
    markable
  }

  def markEpisodeFinished(episode: EpisodeBaseWithPlaybackInfo): Boolean =
    markEpisodesFinished(Traversable(episode)).nonEmpty

  def markEpisodeFinished(episodeId: Long): Unit =
    updatePlaylist(episodeDao.updatePlaybackFinished(episodeId))

  def markThisAndOlderEpisodesFinished(episode: EpisodeBase): Unit =
    updatePlaylist(episodeDao.updatePlaybackFinishedThisAndOlder(episode.podcast, episode.id))

  def canMarkUnfinishedAtLeastOneOf(episodes: Traversable[EpisodeBaseWithPlaybackInfo]): Boolean =
    filterCanMarkUnfinished(episodes).nonEmpty

  def filterCanMarkUnfinished[A <: EpisodeBaseWithPlaybackInfo](episodes: Traversable[A]): Traversable[A] =
    episodes.filter(_.playbackInfo.finished)

  def markEpisodesUnfinished[A <: EpisodeBaseWithPlaybackInfo](episodes: Traversable[A]): Traversable[A] = {
    val markable = filterCanMarkUnfinished(episodes)
    updatePlaylist(episodeDao.updatePlaybackUnfinished(markable.map(_.id)))
    markable
  }

  def markEpisodeUnfinished(episode: EpisodeBaseWithPlaybackInfo): Boolean =
    markEpisodesUnfinished(Traversable(episode)).nonEmpty

  def markEpisodeUnfinished(episodeId: Long): Unit =
    updatePlaylist(episodeDao.updatePlaybackUnfinished(episodeId))

  //
  // playback handling
  //

  def isIdle: Boolean = callServiceIfBound(_.idle).getOrElse(true)

  def isPlaying: Boolean = callServiceIfBound(_.playing).getOrElse(false)

  def isPaused: Boolean = callServiceIfBound(_.paused).getOrElse(false)

  def playingEpisode: Option[EpisodeListItem] = callServiceIfBound(_.playingEpisode).getOrElse(None)

  def chapters: Option[MediaChapterTable] = callServiceIfBound(_.chapters).getOrElse(None)

  def currentChapter: Option[MediaChapter] = callServiceIfBound(_.currentChapter).getOrElse(None)

  def canResume: Boolean = callServiceIfBound(s => s.canResume || (s.canPlay && playingOrNextEpisodeId.isDefined)).getOrElse(playingOrNextEpisodeId.isDefined)

  def canPause: Boolean = callServiceIfBound(_.canPause).getOrElse(false)

  def canSeek: Boolean = callServiceIfBound(_.canSeek).getOrElse(false)

  def canSkipChapter: Boolean = callServiceIfBound(_.canSkipChapter).getOrElse(false)

  def canGoBackChapter: Boolean = callServiceIfBound(_.canGoBackChapter).getOrElse(false)

  def canStop: Boolean = callServiceIfBound(_.canStop).getOrElse(false)

  def play(episode: EpisodeListItem, context: Option[Context] = None): Unit = {
    log.crashLogInfo(s"request to play episode ${episode.uri}")
    showPlaybackViewOnPlaybackStartIfApplicable(episode, context)
    callService(_.play(episode.id))
  }

  def pause() {
    callServiceIfBound(_.pause())
  }

  def resume(context: Option[Context] = None): Unit = {
    if (callServiceIfBound(_.canResume).getOrElse(false)) {
      log.crashLogInfo(s"resume request for already connected playback service")
      if (!showPredictablePlaybackError(context, playingEpisode)) {
        callServiceIfBound(_.resume())
        playingEpisode.foreach(showPlaybackViewOnPlaybackStartIfApplicable(_, context))
      }
    } else if (callServiceIfBound(_.canPlay).getOrElse(!isServiceConnected)) {
      log.crashLogInfo(s"resume request for not yet connected playback service")
      forDefinedPlayingOrNextEpisode { episode =>
        if (!showPredictablePlaybackError(context, Some(episode))) {
          play(episode, context)
        }
      }
    }
  }

  def showPredictablePlaybackError(context: Option[Context], episode: Option[EpisodeBaseWithDownloadInfo]): Boolean = {
    episode match {
      case Some(e) =>
        context match {
          case Some(activity: Activity) if !mediaRouteService.currentDevice.exists(_.isInternetStreamingDevice) =>
            val storageProvider = storagePreferences.storageProvider
            val playbackError = storageProvider.whenReadable(s => e.mediaFile(s)) match {
              case None =>
                Some(PlaybackError(PlaybackError.StorageNotAvailable))
              case Some(file) if e.downloadInfo.fetchedBytes > 0 && !file.exists =>
                episodeDao.inTransaction(episodeDao.resetDownloadInfo(e.id))
                Some(PlaybackError(PlaybackError.FileDoesNotExist))
              case _ =>
                None
            }
            playbackError match {
              case Some(error) =>
                val msg = s"preventing playback due to predictable error $error for file ${episode.map(e => storageProvider.whenReadable(e.mediaFile))}"
                log.crashLogError(msg)
                PlaybackErrorActivity.start(activity, error, episode)
                true
              case _ =>
                false
            }
          case _ =>
            false
        }
      case _ =>
        false
    }
  }

  def fastForward() {
    callServiceIfBound(_.fastForward())
  }

  def rewind() {
    callServiceIfBound(_.rewind())
  }

  def seek(position: Long, commit: Boolean = true) {
    callServiceIfBound(_.seek(position, commit))
  }

  def skipChapter(): Unit = callServiceIfBound(_.skipChapter())

  def goBackChapter(): Unit = callServiceIfBound(_.goBackChapter())

  def skip() {
    if (callServiceIfBound(_.skip()).isEmpty) {
      async {
        playingOrNextEpisodeId.foreach(markEpisodeFinished)
      }
    }
  }

  def stop() {
    callServiceIfBound(_.stop())
  }

  def surface: Option[SurfaceTexture] =
    callServiceIfBound(_.getSurface).getOrElse(None)

  def setSurface(surface: Option[SurfaceTexture]): Unit = {
    callServiceIfBound(_.setSurface(surface))
  }

  def setCareForSurface(care: Boolean): Boolean = {
    callServiceIfBound { service =>
      service.setCareForSurface(care)
      true
    }.getOrElse(false)
  }

  def videoSize: VideoSize =
    callServiceIfBound(_.videoSize).getOrElse(VideoSize(0, 0))

  def audioFxAvailability: AudioFxAvailability.AudioFxAvailability =
    callServiceIfBound(_.audioFxAvailability).getOrElse(AudioFxAvailability.NotForCurrentDataSource)

  def areAudioFxAvailable: Boolean =
    audioFxAvailability == AudioFxAvailability.Available

  def setPlaybackSpeedMultiplier(multiplier: Float): Unit =
    callServiceIfBound(_.setPlaybackSpeedMultiplier(multiplier))

  def playbackSpeedMultiplier: Float =
    callServiceIfBound(_.playbackSpeedMultiplier).getOrElse(1f)

  def setVolumeGain(gain: Float): Unit =
    callServiceIfBound(_.setVolumeGain(gain))

  def volumeGain: Float =
    callServiceIfBound(_.volumeGain).getOrElse(1f)

  def startSleepTimer(mode: SleepTimerMode): Unit =
    callServiceIfBound(_.startSleepTimer(mode))

  def cancelSleepTimer(): Unit =
    callServiceIfBound(_.cancelSleepTimer())

  def sleepTimerMode: SleepTimerMode =
    callServiceIfBound(_.sleepTimerMode).getOrElse(SleepTimerMode.Off)

  //
  // UI handling
  //

  private def showPlaybackViewOnPlaybackStartIfApplicable(episode: EpisodeBase, context: Option[Context]): Unit = {
    import mobi.upod.app.storage.AutoShowPlaybackViewStrategy._

    val activity = context.collect { case a: Activity if !a.isInstanceOf[PlaybackActivity] => a }
    activity foreach { ctx =>
      playbackPreferences.autoShowPlaybackViewStrategy.get match {
        case Always => Try(PlaybackActivity.start(ctx))
        case Video if episode.isVideo => Try(PlaybackActivity.start(ctx))
        case _ =>
      }
    }
  }

  //
  // service
  //

  override protected def onServiceConnected(controller: PlaybackController) {
    super.onServiceConnected(controller)
    controller.addWeakListener(this, false)
  }

  //
  // listener
  //

  protected def fireActiveState(listener: PlaybackListener) {
    def currentEpisodeId = callServiceIfBound(_.episode.map(_.id)).flatten.orElse(playingOrNextEpisodeId)

    val episodeId = currentEpisodeId
    episodeId match {
      case Some(eid) =>
        async(episodeDao.findListItemById(eid)) { episode =>
          // hack: episodeId may have changed in the meanwhile, so we should only call the listener if it is unchanged
          if (episodeId != currentEpisodeId) {
            fireActiveState(listener)
          } else {
            listener.onEpisodeChanged(episode)
            episode.foreach { e =>
              listener.onPlaybackPositionChanged(e)
              if (isPlaying) {
                listener.onPlaybackStarted(e)
              }
            }
            if (areAudioFxAvailable) {
              val speed = playbackSpeedMultiplier
              listener.onAudioEffectsAvailable(true)
              if (speed != 1.0f) {
                listener.onPlaybackSpeedChanged(speed)
              }
            }
            chapters.foreach(listener.onChaptersChanged)
            currentChapter.foreach(chapter => listener.onCurrentChapterChanged(Some(chapter)))
            callServiceIfBound(_.sleepTimerMode).foreach(mode => if (mode != SleepTimerMode.Off) listener.onSleepTimerModeChanged(mode))
          }
        }
      case None =>
        if (asyncInitFinished) {
          listener.onEpisodeChanged(None)
        }
    }
  }

  protected def fireEpisodeIdChanged(episodeId: Option[Long]): Unit = {
    forPlayingOrNextEpisode(e => fire(_.onEpisodeChanged(e)))
  }


  override def onPlaylistChanged(): Unit =
    fire(_.onPlaylistChanged())

  override def onPreparingPlayback(episode: EpisodeBaseWithPlaybackInfo): Unit =
    fire(_.onPreparingPlayback(episode))

  override def onPlaybackStarted(episode: EpisodeBaseWithPlaybackInfo): Unit =
    fire(_.onPlaybackStarted(episode))

  override def onPlaybackPaused(episode: EpisodeBaseWithPlaybackInfo): Unit =
    fire(_.onPlaybackPaused(episode))

  override def onPlaybackStopped(): Unit = {
    if (mediaRouteService.currentDevice.isEmpty) {
      unbindService()
    }
    fire(_.onPlaybackStopped())
  }

  override def onEpisodeChanged(episode: Option[EpisodeBaseWithPlaybackInfo]): Unit =
    fire(_.onEpisodeChanged(episode))

  override def onChaptersChanged(chapters: MediaChapterTable): Unit =
    fire(_.onChaptersChanged(chapters))

  override def onPlaybackPositionChanged(episode: EpisodeBaseWithPlaybackInfo): Unit =
    fire(_.onPlaybackPositionChanged(episode))

  override def onCurrentChapterChanged(chapter: Option[MediaChapter]): Unit =
    fire(_.onCurrentChapterChanged(chapter))

  override def onAudioEffectsAvailable(available: Boolean): Unit =
    fire(_.onAudioEffectsAvailable(available))

  override def onPlaybackSpeedChanged(playbackSpeed: Float): Unit =
    fire(_.onPlaybackSpeedChanged(playbackSpeed))

  override def onEpisodeCompleted(episode: EpisodeBaseWithPlaybackInfo): Unit =
    fire(_.onEpisodeCompleted(episode))

  override def onSleepTimerModeChanged(mode: SleepTimerMode): Unit =
    fire(_.onSleepTimerModeChanged(mode))

  //
  // media route listener
  //

  override def onVolumeGainChanged(gain: Float): Unit =
    fire(_.onVolumeGainChanged(gain))

  override def onMediaRouteDeviceConnected(device: MediaRouteDevice): Unit = {
    device.currentMediaUrl match {
      case Some(mediaUrl) =>
        callService(_.joinRemoteSession(mediaUrl, device.currentPlaybackState))
      case _ =>
        stop()
    }
  }

  override def onMediaRouteDeviceDisconnected(device: MediaRouteDevice): Unit =
    stop()
}
