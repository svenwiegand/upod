package mobi.upod.app.services.download

import java.io.File

import com.escalatesoft.subcut.inject.{BindingModule, Injectable}
import com.evernote.android.job.{Job, JobCreator, JobManager}
import mobi.upod.android.app.BoundServiceConnection
import mobi.upod.android.job.{ConnectedJobRequestBuilder, SimpleJob}
import mobi.upod.android.logging.Logging
import mobi.upod.android.os.PowerManager
import mobi.upod.app.App
import mobi.upod.app.data.{EpisodeBaseWithDownloadInfo, EpisodeListItem, PodcastListItem}
import mobi.upod.app.services.EpisodeService
import mobi.upod.app.services.net.{ConnectionState, ConnectionStateRetriever}
import mobi.upod.app.services.storage.StorageStateListener
import mobi.upod.app.storage._
import mobi.upod.io._
import mobi.upod.util.Observable

import scala.collection.mutable.ListBuffer

class DownloadService(implicit val bindingModule: BindingModule)
  extends BoundServiceConnection[DownloadController]
  with Observable[DownloadListener]
  with DownloadListener
  with StorageStateListener
  with JobCreator
  with Injectable
  with Logging {

  private val JobTagQueueDownload = "download_queue"

  private val app = inject[App]
  private val episodeDao = inject[EpisodeDao]
  private val episodeService = inject[EpisodeService]
  private val downloadPreferences = inject[DownloadPreferences]
  private lazy val storagePreferences = inject[StoragePreferences]
  private lazy val powerManager = new PowerManager(inject[App])
  private lazy val connectionService = inject[ConnectionStateRetriever]

  protected val serviceContext = app
  protected val serviceBinder = DownloadServiceImpl

  private var _downloadingEpisode: Option[EpisodeBaseWithDownloadInfo] = None
  private val _immediateDownloadQueue = ListBuffer[EpisodeBaseWithDownloadInfo]()

  init()

  private def init(): Unit = {

    def initAutomaticDownload(): Unit = {
      JobManager.instance.addJobCreator(this)
      scheduleAutomaticDownload() // start now if something is in the queue...
    }

    initAutomaticDownload()
  }

  def scheduleAutomaticDownload(): Unit = {
    log.info("scheduling automatic download for as soon as required connection is available")
    ConnectedJobRequestBuilder.scheduleImmediate(
      app,
      JobTagQueueDownload,
      downloadPreferences.autoStartDownloadStrategy.get == AutoDownloadStrategy.NonMeteredConnection,
      true
    )
  }


  private def updateDownloadList(startDownloadIfPossible: Boolean, update: => Unit): Unit = {
    episodeDao.inTransaction(update)
    episodeService.fireEpisodeCountChanged()
    fire(_.onDownloadListChanged())
    if (startDownloadIfPossible) {
      scheduleAutomaticDownload()
    }
  }

  def downloadingEpisode: Option[EpisodeBaseWithDownloadInfo] = _downloadingEpisode

  def isDownloading: Boolean = _downloadingEpisode.isDefined || _immediateDownloadQueue.nonEmpty

  def immediateDownloadQueue: Seq[EpisodeBaseWithDownloadInfo] = _immediateDownloadQueue

  def download(episode: EpisodeListItem): Unit = {

    def downloadEpisode(): Unit = {
      episode +=: _immediateDownloadQueue
      episodeService.ensureIsInLibrary(episode)
      callService(_.downloadEpisode(episode, connectionService.isUnmeteredConnection))
    }

    def addEpisodeToImmediateDownloadQueue(): Unit = {
      episodeService.ensureIsInLibrary(episode)
      val downloadQueueIndex = _immediateDownloadQueue.size
      _immediateDownloadQueue += episode
      updateDownloadList(false, episodeDao.insertIntoDownloadList(downloadQueueIndex, episode.episodeId))
    }

    this.synchronized {
      if (!isDownloading)
        downloadEpisode()
      else if (!_downloadingEpisode.exists(_.id == episode.id))
        addEpisodeToImmediateDownloadQueue()
    }
  }

  def downloadQueue(): Unit =
    callService(_.downloadQueue(connectionService.isUnmeteredConnection))

  def stopDownload(): Unit = {
    _immediateDownloadQueue.clear()
    callService(_.stopAllDownloads())
  }

  def buffer(episode: EpisodeListItem): Unit = {
    if (!isDownloading) {
      callService(_.bufferEpisode(episode))
    }
  }

  def stopBuffering(): Unit = {
    callService(_.stopBuffering())
  }

  def addDownloads(ids: Traversable[Long]): Unit =
    updateDownloadList(true, episodeDao.addToDownloadListIfNotAlready(ids))

  def canRemoveAtLeastOneOf(episodes: Traversable[EpisodeListItem]): Boolean =
    filterRemovable(episodes).nonEmpty

  def filterRemovable(episodes: Traversable[EpisodeListItem]): Traversable[EpisodeListItem] = {
    val downloadingId = downloadingEpisode.map(_.id).getOrElse(0)
    val keepPlaylistEpisodes = downloadPreferences.shouldAutoAddPlaylistEpisodes
    val keepLibraryEpisodes = downloadPreferences.shouldAutoAddLibraryEpisodes
    val keepNewEpisodes = downloadPreferences.shouldAutoAddNewEpisodes

    episodes.filter { e =>
      e.downloadInfo.listPosition.isDefined && e.id != downloadingId &&
        !(e.playbackInfo.listPosition.isDefined && keepPlaylistEpisodes) &&
        !(e.library && keepLibraryEpisodes) &&
        !(e.isNew && keepNewEpisodes)
    }
  }

  def removeEpisodes(episodes: Traversable[EpisodeListItem]): Traversable[EpisodeListItem] = {
    val removables = filterRemovable(episodes)
    val removableIds = removables.map(_.id)
    removeFromImmediateQueue(episodes)
    updateDownloadList(false, episodeDao.removeFromDownloadList(removableIds))
    removables
  }

  def asyncUpdateDownloadList(episodes: IndexedSeq[EpisodeListItem]): Unit = {
    AsyncTransactionTask.execute(episodeDao.updateDownloadList(episodes.map(_.episodeId)))
  }

  def deleteDownload(episode: EpisodeListItem): Unit = {
    if (episodeService.canDeleteAtLeastOneDownload(Traversable(episode))) {
      episode.deleteMediaFile(storagePreferences.storageProvider)
      episodeDao.inTransaction(episodeDao.resetDownloadInfo(episode.id))
      episodeService.fireEpisodeCountChanged()
    }
  }

  def deleteAllDownloads(storageProvider: StorageProvider): Unit = {
    log.crashLogWarn("Deleting all downloads!")
    episodeDao.inTransaction {
      val downloadedIds = episodeDao.findDownloadedIds.toSeqAndClose()
      episodeDao.resetDownloads()
      addDownloads(downloadedIds)
      scheduleAutomaticDownload()
    }
    storageProvider.podcastDirectory.deleteRecursive()
  }

  def deleteRecentlyFinished(): Unit = {
    log.info("Deleting all recently finished episodes")
    episodeDao.inTransaction(episodeDao.resetDownloadInfoForRecentlyFinishedEpisodes())
    deleteUnreferencedDownloads()
    episodeService.fireEpisodeCountChanged()
  }

  def deleteRecentlyFinished(podcast: PodcastListItem): Unit = {
    log.info(s"Deleting all recently finished episodes for podcast ${podcast.title}")
    episodeDao.inTransaction(episodeDao.resetDownloadInfoForRecentlyFinishedEpisodes(podcast.id))
    deleteUnreferencedDownloads()
    episodeService.fireEpisodeCountChanged()
  }

  private[download] def deleteUnreferencedDownloads(): Unit = {

    def findReferencedFiles(implicit storageProvider: StorageProvider): Set[File] = {
      val filesDownloadedOrOnDownloadList = {
        val baseDir = storageProvider.podcastDirectory
        episodeDao.findDownloadFilesToKeep.toSetAndClose().map { fileName =>
          new File(baseDir, fileName)
        }
      }
      val filesOnImmediateQueue = _immediateDownloadQueue.map(_.mediaFile(storageProvider)).toSet
      filesDownloadedOrOnDownloadList ++ filesOnImmediateQueue
    }

    def deleteFilesNotIn(keep: Set[File])(implicit storageProvider: StorageProvider): Unit = {
      val downloads = storageProvider.podcastDirectory.listFilesRecursively.toSet
      val unreferenced = downloads -- keep
      log.crashLogInfo(s"Deleting ${unreferenced.size} unreferenced downloads")
      unreferenced.foreach { file =>
        log.crashLogInfo(s"Deleting unreferenced file $file")
        file.delete()
        file.deleteParentIfEmpty()
      }
    }

    def deleteUnreferenced(): Unit = {
      implicit val storageProvider = storagePreferences.storageProvider
      if (storageProvider.writable) {
        val referencedFiles = findReferencedFiles
        deleteFilesNotIn(referencedFiles)
      } else {
        log.crashLogInfo(s"skipping deletion of podcast files on ${storageProvider.id} storage as it isn't writable")
      }
    }

    deleteUnreferenced()
  }

  def applyAutoAddStrategy() {
    val idsForAutoDownloadPodcasts = episodeDao.findNewAutoDownloadEpisodeIds.toSetAndClose()
    val idsForStrategy = downloadPreferences.autoAddDownloadStrategy.get match {
      case AutoAddDownloadStrategy.NewAndLibrary =>
        episodeDao.findUnfinishedNewAndLibraryIds.toSetAndClose()
      case AutoAddDownloadStrategy.Library =>
        episodeDao.findUnfinishedLibraryIds.toSetAndClose()
      case AutoAddDownloadStrategy.Playlist =>
        episodeDao.findPlaylistIds.toSetAndClose()
      case _ =>
        Traversable()
    }
    val ids = idsForAutoDownloadPodcasts ++ idsForStrategy
    if (ids.nonEmpty) {
      updateDownloadList(true, episodeDao.addToDownloadListIfNotAlready(ids, false))
    }
  }

  private def removeFromImmediateQueue(episode: EpisodeBaseWithDownloadInfo): Unit = {
    _immediateDownloadQueue.indexWhere(_.id == episode.id) match {
      case index if index >= 0 => _immediateDownloadQueue.remove(index)
      case _ =>
    }
  }

  private def removeFromImmediateQueue(episodes: TraversableOnce[_ <: EpisodeBaseWithDownloadInfo]): Unit =
    episodes.foreach(removeFromImmediateQueue)

  //
  // download listener implementation
  //

  protected def fireActiveState(listener: DownloadListener): Unit =
    _downloadingEpisode.foreach(listener.onDownloadStarted)

  override protected def onServiceConnected(controller: DownloadController): Unit = {
    super.onServiceConnected(controller)
    controller.addListener(this)
  }

  override def onDownloadListChanged(): Unit = {
    fire(_.onDownloadListChanged())
    episodeService.fireEpisodeCountChanged()
  }

  override def onDownloadStarted(episode: EpisodeBaseWithDownloadInfo): Unit = {

    def ensureAtImmediateQueueStart(): Unit = {
      removeFromImmediateQueue(episode)
      episode +=: _immediateDownloadQueue
    }

    _downloadingEpisode = Some(episode)
    ensureAtImmediateQueueStart()
    fire(_.onDownloadStarted(episode))
  }

  override def onDownloadProgress(episode: EpisodeBaseWithDownloadInfo, bytesPerSecond: Int, remainingMillis: Option[Long]): Unit =
    fire(_.onDownloadProgress(episode, bytesPerSecond, remainingMillis))

  override def onDownloadStopped(episode: EpisodeBaseWithDownloadInfo): Unit = {
    _downloadingEpisode = None
    removeFromImmediateQueue(episode)
    fire(_.onDownloadStopped(episode))
  }

  override def onDownloadsFinished(): Unit = {
    if (_immediateDownloadQueue.nonEmpty) {
      callService(_.downloadEpisode(_immediateDownloadQueue.head, connectionService.isUnmeteredConnection))
    } else {
      log.info("downloads finished -- releasing download service")
      unbindService()
    }
  }

  override def onDownloadsCancelled(error: Throwable): Unit = {
    log.info("downloads cancelled")
    _immediateDownloadQueue.clear()
    connectionService.getConnectionState match {
      case ConnectionState.Unconnected =>
        scheduleAutomaticDownload()
      case ConnectionState.Metered if downloadPreferences.autoStartDownloadStrategy.get == AutoDownloadStrategy.NonMeteredConnection =>
        scheduleAutomaticDownload()
      case _ =>
    }
  }

  //
  // storage listener
  //

  override def onStorageStateChanged(oldState: StorageState.StorageState, newState: StorageState.StorageState): Unit = {
    import mobi.upod.app.storage.StorageState._

    def stopDownloadsIfApplicable(): Unit = if (newState != Writable) {
      callServiceIfBound(_.stopAllDownloads())
    }

    def startAutomaticDownloadIfPossible(): Unit = if (newState == Writable) {
      scheduleAutomaticDownload()
    }

    powerManager.partiallyWaked {
      stopDownloadsIfApplicable()
      startAutomaticDownloadIfPossible()
    }
  }

  //
  // job creator
  //

  override def create(tag: String): Job = tag match {
    case JobTagQueueDownload => SimpleJob withResult {
      val storageAvailable = storagePreferences.storageProvider.writable
      val unmeteredConnectionAvailable = connectionService.isUnmeteredConnection
      val allowAnyConnection = downloadPreferences.autoStartDownloadStrategy.get == AutoDownloadStrategy.AnyConnection
      val connectionAvailable = connectionService.isInternetAvailable && (unmeteredConnectionAvailable || allowAnyConnection)
      if (storageAvailable && connectionAvailable) {
        callService(_.downloadQueue(unmeteredConnectionAvailable))
        Job.Result.SUCCESS
      } else
        Job.Result.FAILURE
    }
    case _ => null
  }
}
