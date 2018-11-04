package mobi.upod.app.services.download

import java.io.InterruptedIOException

import android.app.PendingIntent
import android.content.Intent
import android.support.v4.app.NotificationCompat
import mobi.upod.android.app.{AsyncBoundService, IntegratedNotificationManager, ServiceBinder}
import mobi.upod.android.logging.Logging
import mobi.upod.android.media.MediaFileDurationRetriever
import mobi.upod.android.os.{AsyncObservable, PowerManager}
import mobi.upod.app.data.{EpisodeBaseWithDownloadInfo, EpisodeListItem}
import mobi.upod.app.gui.{MainActivity, MainNavigation}
import mobi.upod.app.services.net.{ConnectionException, ConnectionState, ConnectionStateRetriever}
import mobi.upod.app.services.{EpisodeNotificationBuilder, EpisodeService}
import mobi.upod.app.storage._
import mobi.upod.app.{AppInjection, R}
import mobi.upod.net._
import mobi.upod.util.Duration._
import mobi.upod.util.MinIntervalEventFilter
import mobi.upod.util.StorageSize._
import mobi.upod.util.concurrent.CancelableRunnable

final class DownloadServiceImpl
  extends AsyncBoundService[DownloadController]
  with DownloadController
  with AsyncObservable[DownloadListener] with MinIntervalEventFilter[DownloadListener]
  with DownloadListener
  with IntegratedNotificationManager
  with AppInjection
  with Logging {

  private lazy val episodeDao = inject[EpisodeDao]
  private lazy val downloadService = inject[DownloadService]
  private lazy val connectionService = inject[ConnectionStateRetriever]
  private lazy val storagePreferences = inject[StoragePreferences]
  private lazy val storageProvider = storagePreferences.storageProvider
  private lazy val notificationBuilder = createNotificationBuilder
  private lazy val powerManager = new PowerManager(this)
  private var _downloadingEpisode: Option[EpisodeBaseWithDownloadInfo] = None

  addSynchronousListener(this, false)

  override def onCreate(): Unit = {
    log.info("download service created")
    super.onCreate()
  }

  override def onDestroy(): Unit = {
    log.info("destroying download service")
    cancelNotification()
    super.onDestroy()
  }

  override def onTaskRemoved(rootIntent: Intent): Unit = {
    log.info("uPod task removed -- stopping download")
    stopAllDownloads()
    super.onTaskRemoved(rootIntent)
  }

  private def createNotificationBuilder: EpisodeNotificationBuilder = {
    new EpisodeNotificationBuilder(this).
      setIcon(R.drawable.ic_stat_download).
      setTargetNavigationItem(MainNavigation.downloads).
      addAction(
        R.drawable.ic_action_download_stop,
        R.string.action_stop_download,
        RemoteDownloadActionIntent(getApplicationContext, R.id.action_stop_download))
  }

  private def canDownload: Boolean =
    connectionService.isInternetAvailable && storageProvider.writable

  private def ensureCanDownload(): Unit = {
    cancelErrorNotification()
    if (!connectionService.isInternetAvailable) {
      showErrorNotification(R.string.download_error_title, R.string.connection_error)
      throw new ConnectionException(null)
    }
    if (!storageProvider.writable) {
      showErrorNotification(R.string.download_error_title, getString(R.string.storage_not_writable, storageProvider.storageName))
      throw new StorageException(s"storage ${storageProvider.storageName} not writable")
    }

    val isSpaceLow: Boolean = {
      val availableMb = storageProvider.availableBytes.inMb
      val minimumMb = storagePreferences.minimalFreeMegaBytes.get
      availableMb <= minimumMb
    }
    if (isSpaceLow) {
      showLowSpaceNotificationAndThrowException()
    } 
  }
  
  def showLowSpaceNotificationAndThrowException(): Unit = {
    showErrorNotification(R.string.download_error_not_enough_space, R.string.download_error_not_enough_space_details)
    throw new StorageException(s"too less space on storage ${storageProvider.storageName}")
  }

  def downloadQueue(stopOnMeteredConnection: Boolean) {
    log.crashLogInfo("downloading download queue")
    execute(new EpisodeQueueDownload(stopOnMeteredConnection))
  }

  def downloadEpisode(episode: EpisodeBaseWithDownloadInfo, stopOnMeteredConnection: Boolean) {
    execute(new SingleEpisodeDownload(episode, stopOnMeteredConnection))
  }

  def stopAllDownloads() {
    cancelCurrentTasks()
    fire(_.onDownloadsFinished())
  }

  def bufferEpisode(episode: EpisodeBaseWithDownloadInfo): Unit =
    execute(new EpisodeBuffering(episode))

  override def stopBuffering(): Unit =
    cancelCurrentTaskIf(_.isInstanceOf[EpisodeBuffering])

  private def isBuffering: Boolean = currentTask.exists(_.isInstanceOf[EpisodeBuffering])

  private def doEpisodeDownload(e: EpisodeListItem, stopOnMeteredConnection: Boolean, cancelled: => Boolean): Boolean = {
    var episode = e.copy(downloadInfo =
      e.downloadInfo.copy(attempts = e.downloadInfo.attempts + 1, lastErrorText = None))

    def cancelDownload(): Unit = {
      cancelCurrentTasks()
      throw new InterruptedException("Cancelled download due to wrong connection state")
    }

    def stopIfNecessary(): Unit = {
      val stop = connectionService.getConnectionState match {
        case ConnectionState.Unconnected => true
        case ConnectionState.Metered if stopOnMeteredConnection => true
        case _ => false
      }
      if (stop) {
        log.info(s"new connection state ${connectionService.getConnectionState}: stopping download")
        stopAllDownloads()
      }
    }

    def persistFileNameIfNotSet(): Unit = if (episode.downloadInfo.file.isEmpty) {
      episodeDao.inTransaction {
        episodeDao.updateDownloadFile(
          episode.id,
          EpisodeBaseWithDownloadInfo.mediaFile(episode.media.url, episode.podcast, episode.podcastInfo.title, episode.uri, episode.title, episode.published)
        )
      }
    }

    def updateEpisode(fetchedBytes: Long, length: Long, completed: Boolean = false): EpisodeListItem = {

      def fetchedBlocks =
        fetchedBytes / FileDownloader.ReadBufferSize

      def episodeDuration: Long = {
        if (episode.media.duration > 0)
          episode.media.duration
        else if (completed || fetchedBlocks % 500 == 0)
          MediaFileDurationRetriever.readDuration(e.mediaFile(storagePreferences.storageProvider)).getOrElse(0l)
        else
          0
      }

      val effectiveLength = if (completed) fetchedBytes else length
      val updatedMedia = if (effectiveLength > 0 && effectiveLength != episode.media.length) {
        episode.media.copy(length = effectiveLength, duration = episodeDuration)
      } else if (episode.media.duration == 0) {
        episodeDuration match {
          case duration if duration > 0 => episode.media.copy(duration = duration)
          case _ => episode.media
        }
      } else {
        episode.media
      }

      val updatedDownloadInfo = if (fetchedBytes != episode.downloadInfo.fetchedBytes || completed != episode.downloadInfo.complete)
        episode.downloadInfo.copy(fetchedBytes = fetchedBytes, complete = completed)
      else
        episode.downloadInfo

      episode = if ((updatedMedia ne episode.media) || (updatedDownloadInfo ne episode.downloadInfo))
        episode.copy(media = updatedMedia, downloadInfo = updatedDownloadInfo)
      else
        episode
      episode
    }

    def episodeWithError(error: String): EpisodeListItem = {
      if (canDownload)
        episode.copy(downloadInfo = episode.downloadInfo.copy(lastErrorText = Some(error)))
      else
        episode // assume no error if not internet or storage
    }

    def onProgressUpdate(progress: FileDownloader.Progress) {
      updateEpisode(progress.downloaded, progress.length)
      fireNonIntrusive(_.onDownloadProgress(episode, progress.bytesPerSecond, progress.remainingMillis))
      stopIfNecessary()
    }
    
    def allowNewFileOrFail(expectedLength: Long): Unit = {
      val availableMbAfterDownload = (storageProvider.availableBytes - expectedLength).inMb
      val minimumMb = storagePreferences.minimalFreeMegaBytes.get
      if (availableMbAfterDownload < minimumMb) {
        showLowSpaceNotificationAndThrowException()
      }
    }

    def onFinishedDownload(progress: FileDownloader.Progress): Unit = {
      val downloadedEpisode = updateEpisode(progress.downloaded, progress.length, true)
      inject[EpisodeService].updateChaptersFromFile(downloadedEpisode)
      fire(_.onDownloadStopped(downloadedEpisode))
    }

    fire(_.onDownloadStarted(episode))
    log.info(s"stopOnMeteredConnection=$stopOnMeteredConnection")
    try {
      persistFileNameIfNotSet()
      val progress = FileDownloader.download(
        episode.media.url,
        episode.mediaFile(storagePreferences.storageProvider),
        episode.downloadInfo.fetchedBytes,
        episode.media.length,
        allowNewFileOrFail,
        onProgressUpdate,
        cancelled || Thread.currentThread.isInterrupted)
      onFinishedDownload(progress)
      true
    } catch {
      case ex@(_: InterruptedException | _: InterruptedIOException) =>
        log.crashLogInfo(s"download interrupted")
        fire(_.onDownloadStopped(episode))
        throw ex
      case ex: Throwable =>
        log.crashLogError(s"failed to download episode '${episode.title}'", ex)
        fire(_.onDownloadStopped(episodeWithError(DownloadError(this, ex))))
        throw ex
    }
  }

  def persistDownloadInfo(episode: EpisodeBaseWithDownloadInfo) {
    episodeDao.inTransaction {
      episodeDao.updateDownloadInfo(
        episode.id,
        episode.downloadInfo.fetchedBytes,
        episode.media.length,
        episode.media.duration,
        episode.downloadInfo.complete,
        episode.downloadInfo.attempts,
        episode.downloadInfo.lastErrorText)
    }
  }

  def cancelErrorNotification(): Unit =
    notificationManager.cancel(R.string.download_error_title)

  def showErrorNotification(title: Int, content: String): Unit = {
    val notification = new NotificationCompat.Builder(this).
      setSmallIcon(R.drawable.ic_stat_error).
      setContentTitle(getString(title)).
      setContentText(content).
      setContentIntent(PendingIntent.getActivity(this, 0, MainActivity.intent(this, MainNavigation.downloads), PendingIntent.FLAG_ONE_SHOT)).
      build()
    notificationManager.notify(R.string.download_error_title, notification)
  }

  def showErrorNotification(title: Int, content: Int): Unit =
    showErrorNotification(title, getString(content))

  private def showStartNotification(episode: EpisodeBaseWithDownloadInfo): Unit = if (!isBuffering) {
    val notification = notificationBuilder.
      setEpisode(episode).
      setIndeterminateProgress().
      setContentText(getString(R.string.download_preparing))
    startForeground(notification)
  }

  private def updateNotificationProgress(episode: EpisodeBaseWithDownloadInfo, bytesPerSecond: Int, remainingMillis: Option[Long]): Unit = if (!isBuffering) {
    def fetchedMbs: java.lang.Double = episode.downloadInfo.fetchedBytes.toDouble.inMb
    def kbPerSecond: java.lang.Double = bytesPerSecond.toDouble.inKb

    def fullProgressString: String = {
      val overallMbs: java.lang.Double = episode.media.length.toDouble.inMb
      val remainingTime = remainingMillis.get.formatHoursMinutesAndSeconds
      getString(R.string.download_progress, fetchedMbs, overallMbs, kbPerSecond, remainingTime)
    }

    def indeterminateProgressString: String =
      getString(R.string.download_progress_indeterminate, fetchedMbs, kbPerSecond)

    val deterministic = remainingMillis.isDefined

    if (deterministic)
      notificationBuilder.setProgress(episode.downloadInfo.fetchedBytes.toInt, episode.media.length.toInt)
    else
      notificationBuilder.setIndeterminateProgress()

    val progressString = if (deterministic) fullProgressString else indeterminateProgressString
    notificationBuilder.
      setContentText(progressString)
    updateNotification(notificationBuilder)
  }

  override def onDownloadStarted(episode: EpisodeBaseWithDownloadInfo) {
    log.crashLogInfo(s"downloading episode '${episode.title}'. Already have ${episode.downloadInfo.fetchedBytes} of ${episode.media.length} Bytes (last error: ${episode.downloadInfo.lastErrorText})")
    log.debug(s"notificationID=$notificationId")
    _downloadingEpisode = Some(episode)
    showStartNotification(episode)
    persistDownloadInfo(episode)
  }

  override def onDownloadProgress(episode: EpisodeBaseWithDownloadInfo, bytesPerSecond: Int, remainingMillis: Option[Long]) {
    log.debug(s"download status ${episode.downloadInfo.fetchedBytes.inKb}/${episode.media.length.inKb} kB at ${bytesPerSecond.inKb} kB/s")
    _downloadingEpisode = Some(episode)
    updateNotificationProgress(episode, bytesPerSecond, remainingMillis)
    persistDownloadInfo(episode)
  }

  override def onDownloadStopped(episode: EpisodeBaseWithDownloadInfo) {
    persistDownloadInfo(episode)
    log.crashLogInfo(s"stopped downloading episode '${episode.title}' at ${episode.downloadInfo.fetchedBytes} of ${episode.media.length} Bytes (last error code: ${episode.downloadInfo.lastErrorText})")
    log.debug(s"notificationID=$notificationId")
    stopForeground()
    _downloadingEpisode = None
  }

  protected def fireActiveState(listener: DownloadListener) {
    _downloadingEpisode.foreach(listener.onDownloadStarted)
  }

  private class EpisodeQueueDownload(stopOnMeteredConnection: Boolean) extends CancelableRunnable {

    override def run(): Unit = {
      powerManager.partiallyWaked {
        log.info("Starting episode queue download (acquired wake lock)")
        try {
          downloadService.deleteUnreferencedDownloads()
          doEpisodeQueueDownload(Set(), stopOnMeteredConnection)
          fire(_.onDownloadsFinished())
        } catch {
          case ex: Throwable =>
            log.warn("episode queue download cancelled", ex)
            fire(_.onDownloadsCancelled(ex))
        } finally log.info("Finished episode queue download (released wake lock)")
      }
    }

    private def doEpisodeQueueDownload(triedEpisodeIds: Set[Long], stopOnMeteredConnection: Boolean): Unit = if (!cancelled) {
      ensureCanDownload()
      episodeDao.findFirstDownloadEpisode(triedEpisodeIds) match {
        case Some(episode) =>
          if (doEpisodeDownload(episode, stopOnMeteredConnection, cancelled))
            episodeDao.inTransaction(episodeDao.removeFromDownloadList(Traversable(episode.id)))
          else
            episodeDao.inTransaction(episodeDao.addToDownloadListEnd(Traversable(episode.id)))
          fire(_.onDownloadListChanged())
          doEpisodeQueueDownload(triedEpisodeIds + episode.id, stopOnMeteredConnection)
        case None =>
          log.crashLogInfo("download queue is empty")
      }
    }
  }

  private class SingleEpisodeDownload(episode: EpisodeBaseWithDownloadInfo, stopOnMeteredConnection: Boolean) extends CancelableRunnable {

    def run(): Unit = {
      powerManager.partiallyWaked {
        episodeDao.inTransaction {
          episodeDao.insertAtStartOfDownloadList(episode.episodeId)
        }
        try {
          downloadService.deleteUnreferencedDownloads()
          ensureCanDownload()
          episodeDao.findListItemById(episode.id) match {
            case Some(e) =>
              fire(_.onDownloadListChanged())
              if (doEpisodeDownload(e, stopOnMeteredConnection, cancelled)) {
                episodeDao.inTransaction(episodeDao.removeFromDownloadList(Traversable(e.id)))
              }
            case _ =>
              log.crashLogWarn(s"not able to find episode with id ${episode.id}")
          }
          fire(_.onDownloadListChanged())
          fire(_.onDownloadsFinished())
        } catch {
          case ex: Throwable =>
            log.warn("single episode download cancelled", ex)
            fire(_.onDownloadsCancelled(ex))
        }
      }
    }
  }

  private class EpisodeBuffering(episode: EpisodeBaseWithDownloadInfo) extends CancelableRunnable {

    override def run(): Unit = {
      powerManager.partiallyWaked {
        try {
          ensureCanDownload()
          episodeDao.findListItemById(episode.id) match {
            case Some(e) =>
              doEpisodeDownload(e, connectionService.isUnmeteredConnection, cancelled)
            case _ =>
              log.crashLogWarn(s"not able to find episode with id ${episode.id}")
          }
          fire(_.onDownloadsFinished())
        } catch {
          case ex: Throwable =>
            log.warn("episode buffering cancelled", ex)
            fire(_.onDownloadsFinished())
        }
      }
    }
  }
}

object DownloadServiceImpl extends ServiceBinder[DownloadController, DownloadServiceImpl]