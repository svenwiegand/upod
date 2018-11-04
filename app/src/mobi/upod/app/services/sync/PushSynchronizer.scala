package mobi.upod.app.services.sync

import com.escalatesoft.subcut.inject.{BindingModule, Injectable}
import mobi.upod.android.logging.Logging
import mobi.upod.app.R
import mobi.upod.app.data.EpisodeStatus.EpisodeStatus
import mobi.upod.app.data.{EpisodeReference, EpisodeStatus}
import mobi.upod.app.services.device.DeviceIdService
import mobi.upod.app.storage._
import org.joda.time.DateTime

import scala.annotation.tailrec

class PushSynchronizer(syncer: Syncer, verifySyncState: Boolean, syncConflictResolution: Option[SyncConflictResolution.Value])(implicit val bindingModule: BindingModule) extends Logging with Injectable {
  private val internalSyncPreferences = inject[InternalSyncPreferences]

  private lazy val context = inject[mobi.upod.app.App]
  private lazy val database = inject[Database]
  private lazy val subscriptionChangeDao = inject[SubscriptionChangeDao]
  private lazy val episodeListChangeDao = inject[EpisodeListChangeDao]
  private lazy val episodeDao = inject[EpisodeDao]

  private val cloudSyncEnabled = inject[SyncService].isCloudSyncEnabled

  val PageSize = 100

  def lastPushSyncTimestamp =
    internalSyncPreferences.lastPushSyncTimestamp.option.getOrElse(new DateTime(1))

  def sync(progressIndicator: SyncProgressIndicator, commitChanges: Boolean): Unit = {
    if (internalSyncPreferences.episodeUriState.get == EpisodeUriState.LocalUriUpdateRequired) {
      log.warn("skipping push sync, because local uri update is required")
    } else {
      implicit val pi = progressIndicator
      prepareSync()
      if (internalSyncPreferences.pushSyncRequired) {
        implicit val syncTime = DateTime.now()
        progressIndicator.updateProgress(0, calculateMaxProgress, context.getString(R.string.sync_notification_push))
        doSync(commitChanges)
        resetSyncPreferences()
      } else {
        progressIndicator.updateProgressToMax()
      }
    }
  }

  private def prepareSync()(implicit progressIndicator: SyncProgressIndicator): Unit = if (cloudSyncEnabled) {

    def deleteOutdatedPushState(remoteTimestamp: Option[DateTime]): Unit = {
      log.warn("SYNCSTATUS: deleting outdated push state")
      resetSyncPreferences()
      val remoteStatusTimestamp = remoteTimestamp getOrElse DateTime.now
      database.inTransaction {
        subscriptionChangeDao.deleteUntil(remoteStatusTimestamp)
        episodeListChangeDao.deleteUntil(remoteStatusTimestamp)
      }
      internalSyncPreferences.playlistUpdated := false
      internalSyncPreferences.identitySettingsUpdated := false
      internalSyncPreferences.lastPushSyncTimestamp := remoteStatusTimestamp
    }

    def deleteServerState(): Unit = {
      log.warn("SYNCSTATUS: deleting remote data")
      syncer.deleteSyncData()
    }

    def prepareFullPush(): Unit = {
      database.inTransaction {
        subscriptionChangeDao.replaceAllWithCurrentSubscriptions()
        episodeListChangeDao.replaceAllWithCurrentEpisodeStatus()
      }
      internalSyncPreferences.playlistUpdated := true
      internalSyncPreferences.identitySettingsUpdated := true
      internalSyncPreferences.pushSyncRequired := true
      internalSyncPreferences.lastPushSyncTimestamp := new DateTime(0l)
    }

    if (verifySyncState) {
      (syncConflictResolution, SyncStatus.request(syncer)) match {
        case (Some(SyncConflictResolution.UseDeviceState), _) =>
          deleteServerState()
          prepareFullPush()
        case (Some(SyncConflictResolution.UseServerState), syncStatus) => syncStatus match {
          case ServerAhead(_, remoteTimestamp) => deleteOutdatedPushState(Some(remoteTimestamp))
          case _ => deleteOutdatedPushState(None)
        }
        case (_, ServerAhead(deviceEmpty, remoteTimestamp)) =>
          if (deviceEmpty)
            deleteOutdatedPushState(Some(remoteTimestamp))
          else
            throw new OutOfSyncException("server ahead")
        case (_, DeviceAhead(serverEmpty)) =>
          if (serverEmpty) {
            deleteServerState()
            prepareFullPush()
          } else {
            throw new OutOfSyncException("device ahead")
          }
        case _ =>
          log.info("SYNCSTATUS: in sync")
      }
    }
    progressIndicator.increaseProgress()
  }

  private def calculateMaxProgress(implicit syncTime: DateTime): Int = {

    def numUpdateCalls(entriesToPush: Int): Int = {
      val nonFullUpdateCallCorrection = if ((entriesToPush % PageSize) == 0) 0 else 1
      entriesToPush / PageSize + nonFullUpdateCallCorrection
    }

    def numEpisodeStatusUpdateCalls(status: EpisodeStatus): Int =
      numUpdateCalls(episodeListChangeDao.findCount(status, syncTime))

    def numPlaybackInfoUpdateCalls: Int =
      numUpdateCalls(episodeDao.findChangedPlaybackPositionsCount(lastPushSyncTimestamp))

    if (cloudSyncEnabled) {
      1 + // delete episodes for deleted subscriptions
      numEpisodeStatusUpdateCalls(EpisodeStatus.NoLongerAvailable) +
      numEpisodeStatusUpdateCalls(EpisodeStatus.Finished) +
      numEpisodeStatusUpdateCalls(EpisodeStatus.Library) +
      numEpisodeStatusUpdateCalls(EpisodeStatus.New) +
      numEpisodeStatusUpdateCalls(EpisodeStatus.Starred) +
      1 + // removed subscriptions
      1 + // added subscriptions
      1 + // delete unreferenced episodes
      numPlaybackInfoUpdateCalls + // playback infos
      1 + // playlist
      1 + // settings
      1 // sync timestamp
    } else {
      0
    }
  }

  private def doSync(commitChanges: Boolean)(implicit syncTime: DateTime, progressIndicator: SyncProgressIndicator): Unit = {
    progressIndicator.increaseProgress()

    if (cloudSyncEnabled) {
      syncEpisodeStatus()
      syncSubscriptions()
      deleteUnreferencedEpisodes()
      syncPlaybackInfos()
      syncPlaylist()
      syncSettings()
      syncSyncTimestamp(syncTime)
      syncer.persistSyncStatus()

      if (commitChanges) {
        progressIndicator.updateProgress(context.getString(R.string.sync_notification_sync))
        syncer.commitChanges()
        progressIndicator.increaseProgress()
      }
    }
  }

  private def syncSubscriptions()(implicit syncTime: DateTime, progressIndicator: SyncProgressIndicator): Unit = {

    def syncRemovedSubscriptions(): Unit = {
      val removedSubscriptions = subscriptionChangeDao.findUnsubscribed(syncTime).toSeqAndClose()
      if (removedSubscriptions.nonEmpty) {
        syncer.deleteSubscriptions(removedSubscriptions)
      }
      progressIndicator.increaseProgress()
    }

    def syncAddedAndChanged(): Unit = {
      val addedAndChangedSubscriptions = subscriptionChangeDao.findSubscribedAndChanged(syncTime).toSeqAndClose()
      if (addedAndChangedSubscriptions.nonEmpty) {
        syncer.putSubscriptions(addedAndChangedSubscriptions)
      }
      progressIndicator.increaseProgress()
    }

    syncRemovedSubscriptions()
    syncAddedAndChanged()
    subscriptionChangeDao.inTransaction(subscriptionChangeDao.deleteUntil(syncTime))
  }

  private def syncEpisodeStatus()(implicit syncTime: DateTime, progressIndicator: SyncProgressIndicator): Unit = {

    @tailrec
    def syncEpisodesInStatus(status: EpisodeStatus, sync: Seq[EpisodeReference] => Unit): Unit = {
      val (episodes, maxRowId) = episodeListChangeDao.findLimited(status, syncTime, PageSize)
      if (episodes.nonEmpty) {
        sync(episodes)
        episodeListChangeDao.inTransaction(episodeListChangeDao.delete(status, maxRowId))
        progressIndicator.increaseProgress()

        if (episodes.size == PageSize)
          syncEpisodesInStatus(status, sync)
      }
    }

    def syncDeletedPodcastEpisodes(): Unit = {
      val podcasts = subscriptionChangeDao.findDeletedPodcasts(syncTime).toSeqAndClose()
      syncer.deletePodcastEpisodes(podcasts)
      progressIndicator.increaseProgress()
    }

    def syncNewEpisodes(): Unit =
      syncEpisodesInStatus(EpisodeStatus.New, syncer.putNewEpisodes(_, syncTime))

    def syncLibraryEpisodes(): Unit =
      syncEpisodesInStatus(EpisodeStatus.Library, syncer.putLibraryEpisodes(_, syncTime))

    def syncStarredEpisodes(): Unit =
      syncEpisodesInStatus(EpisodeStatus.Starred, syncer.putStarredEpisodes(_, syncTime))

    def syncFinishedEpisodes(): Unit =
      syncEpisodesInStatus(EpisodeStatus.Finished, syncer.putFinishedEpisodes(_, syncTime))

    def syncNoLongerAvailableEpisodes(): Unit =
      syncEpisodesInStatus(EpisodeStatus.NoLongerAvailable, syncer.deleteEpisodes)

    episodeListChangeDao.inTransaction(episodeListChangeDao.deleteWithoutPodcastInfo())
    syncDeletedPodcastEpisodes()
    syncNoLongerAvailableEpisodes()
    syncNewEpisodes()
    syncLibraryEpisodes()
    syncStarredEpisodes()
    syncFinishedEpisodes()
  }

  private def deleteUnreferencedEpisodes()(implicit progressIndicator: SyncProgressIndicator): Unit = {
    syncer.deleteUnreferencedEpisodes()
    progressIndicator.increaseProgress()
  }

  private def syncPlaybackInfos()(implicit syncTime: DateTime, progressIndicator: SyncProgressIndicator): Unit = {
    val since = lastPushSyncTimestamp

    @tailrec
    def sync(page: Int = 0): Unit = {
      val updatedPlaybackInfos: Seq[EpisodePlaybackInfo] =
        episodeDao.findChangedPlaybackPositions(since, PageSize, page * PageSize).toSeqAndClose()

      if (updatedPlaybackInfos.nonEmpty) {
        syncer.putPlaybackInfos(updatedPlaybackInfos)
        progressIndicator.increaseProgress()
        if (updatedPlaybackInfos.size >= PageSize) {
          sync(page + 1)
        }
      }
    }

    sync()
  }

  private def syncPlaylist()(implicit progressIndicator: SyncProgressIndicator): Unit = if (internalSyncPreferences.playlistUpdated) {
    val playlist = episodeDao.findPlaylistEpisodeReferences.toSeqAndClose()
    syncer.putPlaylist(playlist)
    progressIndicator.increaseProgress()
    internalSyncPreferences.playlistUpdated := false
  }

  private def syncSettings()(implicit progressIndicator: SyncProgressIndicator): Unit = if (internalSyncPreferences.identitySettingsUpdated) {
    val settings = IdentitySettings.apply(bindingModule)
    syncer.putSettings(settings)
    progressIndicator.increaseProgress()
    internalSyncPreferences.identitySettingsUpdated := false
  }

  private def syncSyncTimestamp(syncTime: DateTime)(implicit progressIndicator: SyncProgressIndicator): Unit = {
    syncer.putDeviceSyncTimestamp(inject[DeviceIdService].getDeviceId, syncTime)
    internalSyncPreferences.lastPushSyncTimestamp := syncTime
    progressIndicator.increaseProgress()
  }

  private def resetSyncPreferences(): Unit = {
    internalSyncPreferences.pushSyncRequired := false
    internalSyncPreferences.playlistUpdated := false
    internalSyncPreferences.identitySettingsUpdated := false
    internalSyncPreferences.episodeUriState := EpisodeUriState.UpToDate
  }
}

private object PushSynchronizer {
}