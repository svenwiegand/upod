package mobi.upod.app.services

import java.io.IOException

import com.escalatesoft.subcut.inject.{BindingModule, Injectable}
import mobi.upod.android.logging.Logging
import mobi.upod.android.os.{AsyncTask, AsyncObservable}
import mobi.upod.app.data.EpisodeListItem
import mobi.upod.app.services.download.DownloadService
import mobi.upod.app.services.playback.PlaybackService
import mobi.upod.app.services.sync.SyncService
import mobi.upod.app.storage.{DownloadPreferences, EpisodeDao, StoragePreferences, UiPreferences}
import mobi.upod.media.{MediaChapterRetriever, MediaChapterTable}

import scala.concurrent.Future

final class EpisodeService(implicit val bindingModule: BindingModule)
  extends AsyncObservable[EpisodeListener]
  with Injectable
  with Logging {

  private lazy val episodeDao = inject[EpisodeDao]
  private lazy val syncService = inject[SyncService]
  private lazy val downloadService = inject[DownloadService]
  private lazy val playService = inject[PlaybackService]
  private lazy val downloadPreferences = inject[DownloadPreferences]
  private lazy val storagePreferences = inject[StoragePreferences]
  private lazy val uiPreferences = inject[UiPreferences]

  protected def fireActiveState(listener: EpisodeListener) {
    listener.onEpisodeCountChanged()
  }

  def skipNew(skipNew: Boolean, force: Boolean = false): Unit = {
    if (skipNew != uiPreferences.skipNew.get || force) {
      uiPreferences.skipNew := skipNew
      if (skipNew) {
        episodeDao.inTransaction(episodeDao.addAllNewEpisodesToLibrary())
        fireEpisodeCountChanged()
      }
      syncService.identitySettingsUpdated()
    }
  }

  def canDeleteAtLeastOneDownload(episodes: Traversable[EpisodeListItem]): Boolean =
    filterDeletableDownloads(episodes).nonEmpty

  def filterDeletableDownloads(episodes: Traversable[EpisodeListItem]): Traversable[EpisodeListItem] = {
    val playingId = playService.playingEpisode.map(_.id).getOrElse(0)
    val downloadingId = downloadService.downloadingEpisode.map(_.id).getOrElse(0)
    episodes.filter(e => e.downloadInfo.fetchedBytes > 0 && e.id != playingId && e.id != downloadingId)
  }

  def addToLibrary(ids: Traversable[Long]) {

    def addToDownloadListIfApplicable(ids: Traversable[Long]) {
      if (downloadPreferences.shouldAutoAddLibraryEpisodes) {
        downloadService.addDownloads(ids)
      }
    }

    if (ids.nonEmpty) {
      episodeDao.inTransaction {
        episodeDao.addToLibrary(ids)
        addToDownloadListIfApplicable(ids)
      }
      fireEpisodeCountChanged()
    }
    syncService.pushSyncRequired()
  }

  def addUnfinishedToLibrary(podcast: Long): Unit =
    episodeDao.inTransaction(episodeDao.addUnfinishedToLibrary(podcast))

  def ensureIsInLibrary(episode: EpisodeListItem): Unit = if (!episode.library) {
    addToLibrary(Traversable(episode.id))
  }

  def canChangeStar(episode: EpisodeListItem, star: Boolean): Boolean =
    episode.starred != star

  def canChangeStarFor(episodes: Traversable[EpisodeListItem], star: Boolean): Boolean =
    filterStarChangableEpisodes(episodes, star).nonEmpty

  def filterStarChangableEpisodes(episodes: Traversable[EpisodeListItem], star: Boolean): Traversable[EpisodeListItem] =
    episodes.filter(canChangeStar(_, star))

  def starEpisodes(episodes: Traversable[EpisodeListItem], star: Boolean): Traversable[EpisodeListItem] = {
    val affectedEpisodes = filterStarChangableEpisodes(episodes, star)
    val ids = affectedEpisodes.map(_.id)
    if (ids.nonEmpty) {
      episodeDao.inTransaction(episodeDao.starEpisodes(ids, star))
      fireEpisodeCountChanged()
      syncService.pushSyncRequired()
    }
    affectedEpisodes
  }

  //
  // chapter stuff
  //

  private var chapterTableCache: Option[(Long, MediaChapterTable)] = None

  private def cachedChapters(episodeId: Long): Option[MediaChapterTable] = synchronized {
    chapterTableCache.find(_._1 == episodeId).map(_._2)
  }

  private def cacheChapters(episodeId: Long, chapters: MediaChapterTable) = synchronized {
    chapterTableCache = Some(episodeId -> chapters)
    chapters
  }

  private def updateChapterCacheForEpisode(episodeId: Long, chapters: MediaChapterTable) = synchronized {
    if (chapterTableCache.exists(_._1 == episodeId)) {
      cacheChapters(episodeId, chapters)
    }
  }

  /** Reads the chapters for the specified episode from the episode's media file and persists them in the database.
    *
    * Don't call from UI thread.
    *
    * @param episode the episode to retrieve the chapters for
    * @return the chapters retrieved from the file or an empty section if the file does not contain chapters.
    */
  def updateChaptersFromFile(episode: EpisodeListItem): MediaChapterTable = {
    val chapters: MediaChapterTable = try {
      log.info(s"fetching chapters for episode ${episode.media.url} with mime type ${episode.media.mimeType} (fallback: ${episode.media.mimeTypeByFileExtension})")
      val file = episode.mediaFile(storagePreferences.storageProvider)
      val chapterTable = MediaChapterRetriever.parse(file, episode.media.mimeType.toString, episode.media.mimeTypeByFileExtension.map(_.toString))
      log.info(s"chapter table contains ${chapterTable.size} entries")
      episodeDao.inTransaction(episodeDao.updateChapters(episode.id, chapterTable.chapters))
      chapterTable
    } catch {
      case ex: IOException =>
        log.error(s"failed to retrieve chapters from file ${episode.downloadInfo.file}", ex)
        episodeDao.inTransaction(episodeDao.resetChapters(episode.id))
        MediaChapterTable()
      case ex: Throwable =>
        log.crashLogError(s"failed to retrieve chapters for episode ${episode.media.url} from file ${episode.downloadInfo.file}", ex)
        episodeDao.inTransaction(episodeDao.resetChapters(episode.id))
        MediaChapterTable()
    }
    updateChapterCacheForEpisode(episode.id, chapters)
    chapters
  }

  /** Retrieves the chapters for the specified episode.
    *
    * Trying to retrieve chapters in the following order
    *
    * 1. Cache
    * 2. database
    * 3. media file (retrieved chapters are written to the database in this scenario)
    *
    * This may result in IO operations so don't call from UI thread.
    *
    * @param episode the episode to retrieve the chapters for
    * @return the chapteres of this episode or an empty section if this episode does not provide chapters.
    */
  def getChapters(episode: EpisodeListItem): MediaChapterTable = {
    cachedChapters(episode.id) match {
      case Some(chapters) => chapters
      case None =>
        val chapters = episodeDao.findChapters(episode.id) match {
          case Some(chaps) => MediaChapterTable(chaps.toIndexedSeq)
          case None => updateChaptersFromFile(episode)
        }
        cacheChapters(episode.id, chapters)
    }
  }

  /** Efficiently retrieve chapters from the UI thread.
    *
    * If the chapters are available from the cache a successful `Future` is returned without any asynchronous processing.
    * Otherwise a future calling [[getChapters]] is returned.
    *
    * @param episode the episode to retrieve the chapters for
    * @return a `Future` providing the chapters either immediately (from the cache) or in the future otherwise.
    */
  def futureChapters(episode: EpisodeListItem): Future[MediaChapterTable] = {
    cachedChapters(episode.id) match {
      case Some(chapters) => Future.successful(chapters)
      case None => AsyncTask.future(getChapters(episode))
    }
  }

  //
  // events
  //

  private[services] def fireEpisodeCountChanged() {
    fire(_.onEpisodeCountChanged())
  }
}
