package mobi.upod.app.services.sync.gdrive

import java.io.Closeable
import java.net.URL

import android.content.Context
import android.os.Bundle
import com.github.nscala_time.time.Imports._
import com.google.android.gms.common.api.{ResultCallback, Status}
import mobi.upod.android.logging.Logging
import mobi.upod.android.os.AsyncTask.AsyncTaskExecutionContext
import mobi.upod.app.AppInjection
import mobi.upod.app.data.{EpisodeReference, EpisodeStatus}
import mobi.upod.app.services.cloudmessaging.CloudMessagingService
import mobi.upod.app.services.sync._
import mobi.upod.app.storage.DatabaseHelper
import mobi.upod.data.MappingProvider
import mobi.upod.data.json.{JsonReader, JsonStreamReader, JsonWriter}
import mobi.upod.util.Cursor
import org.joda.time.DateTime

import scala.concurrent.Future
import scala.util.{Failure, Success}

class GDriveSyncer(gdrive: GDriveClient) extends Syncer with Closeable with AppInjection with Logging {
  private val dbHelper = inject[DatabaseHelper]
  private lazy val cloudMessagingService = inject[CloudMessagingService]
  private val subscriptionDao = new SubscriptionSyncDao(dbHelper)
  private val episodeStatusDao = new EpisodeStatusSyncInfoDao(dbHelper)

  init()

  private def init(): Unit = {

    def initDao[A](fileName: String, dao: SyncDao[A], mappingProvider: MappingProvider[A]): Unit = {
      dao.create()
      gdrive.readJsonFile(fileName, reader => dao.importItems(JsonStreamReader(mappingProvider, reader)))
    }

    gdrive.ensureSynced()
    subscriptionDao inTransaction {
      initDao("subscriptions.json", subscriptionDao, Subscription)
      initDao("episodes.json", episodeStatusDao, EpisodeStatusSyncInfo)
    }
  }

  override def close(): Unit = {
    subscriptionDao inTransaction {
      subscriptionDao.destroy()
      episodeStatusDao.destroy()
    }
  }


  override def persistSyncStatus(): Unit = {

    def export[A](fileName: String, dao: SyncDao[A], mappingProvider: MappingProvider[A]): Unit = {
      gdrive.createOrUpdateJsonFile(fileName, { writer =>
        mobi.upod.io.forCloseable(dao.exportItems) { items =>
          JsonWriter(mappingProvider).writeJson(items, writer)
        }
      })
    }

    subscriptionDao inTransaction {
      export("subscriptions.json", subscriptionDao, Subscription)
      export("episodes.json", episodeStatusDao, EpisodeStatusSyncInfo)
    }
  }

  override def commitChanges(): Unit = {
    gdrive.ensureSynced()
    cloudMessagingService.sendCrossDeviceSyncRequest()
  }

  override def deleteSyncData(): Unit =
    gdrive.emptyAppFolder()

  override def getLastSyncTimestamp: Option[DateTime] = {
    val deviceFiles = gdrive.findJsonFiles("^device_.*\\.json")
    val syncTimestamps = deviceFiles.map(gdrive.readFile(_, JsonReader(DeviceSyncInfo).readObject)).map(_.lastSync)
    if (syncTimestamps.nonEmpty) Some(syncTimestamps.max) else None
  }

  override def getSettings: Option[IdentitySettings] =
    gdrive.readJsonFile("settings.json", JsonReader(IdentitySettings).readObject)

  override def putSettings(settings: IdentitySettings): Unit =
    gdrive.createOrUpdateJsonFile("settings.json", JsonWriter(IdentitySettings).writeJson(settings, _))

  //
  // device
  //

  override def getDeviceSyncTimestamp(deviceId: String): Option[DateTime] =
    gdrive.readJsonFile(s"device_$deviceId.json", JsonReader(DeviceSyncInfo).readObject).map(_.lastSync)

  override def putDeviceSyncTimestamp(deviceId: String, syncTimestamp: DateTime): Unit = {
    val syncInfo = DeviceSyncInfo(syncTimestamp)
    gdrive.createOrUpdateJsonFile(s"device_$deviceId.json", JsonWriter(DeviceSyncInfo).writeJson(syncInfo, _))
  }

  //
  // subscriptions
  //

  override def getSubscriptions: Cursor[Subscription] =
    subscriptionDao.exportItems

  override def putSubscriptions(subscriptions: Seq[Subscription]): Unit = subscriptionDao inTransaction {
    subscriptionDao.save(subscriptions)
  }

  override def deleteSubscriptions(subscriptions: Seq[URL]): Unit = subscriptionDao inTransaction {
    subscriptionDao.deleteSubscriptions(subscriptions)
  }

  override def getEpisodePodcasts(changedAfter: Option[DateTime]): Cursor[URL] =
    episodeStatusDao.getPodcastUrls

  //
  // episode status
  //

  override def getEpisodeStatus(podcast: URL, from: Option[DateTime]): Cursor[EpisodeStatusSyncInfo] = from match {
    case Some(startDate) => episodeStatusDao.get(podcast, startDate)
    case _ => episodeStatusDao.get(podcast)
  }

  override def putNewEpisodes(episodes: Seq[EpisodeReference], timestamp: DateTime): Unit = subscriptionDao inTransaction {
    episodeStatusDao.updateStatus(episodes, EpisodeStatus.New, timestamp)
  }

  override def putLibraryEpisodes(episodes: Seq[EpisodeReference], timestamp: DateTime): Unit = subscriptionDao inTransaction {
    episodeStatusDao.updateStatus(episodes, EpisodeStatus.Library, timestamp)
  }

  override def putStarredEpisodes(episodes: Seq[EpisodeReference], timestamp: DateTime): Unit = subscriptionDao inTransaction {
    episodeStatusDao.updateStatus(episodes, EpisodeStatus.Starred, timestamp)
  }

  override def putFinishedEpisodes(episodes: Seq[EpisodeReference], timestamp: DateTime): Unit = subscriptionDao inTransaction {
    episodeStatusDao.updateStatus(episodes, EpisodeStatus.Finished, timestamp)
  }

  override def deleteEpisodes(episodes: Seq[EpisodeReference]): Unit = subscriptionDao inTransaction {
    episodeStatusDao.delete(episodes)
  }

  override def deletePodcastEpisodes(podcasts: Seq[URL]): Unit = subscriptionDao inTransaction {
    episodeStatusDao.deleteByPodcast(podcasts)
  }

  override def deleteUnreferencedEpisodes(): Unit = subscriptionDao inTransaction {
    episodeStatusDao.deleteUnreferenced()
  }

  override def putPlaybackInfos(playbackInfos: Seq[EpisodePlaybackInfo]): Unit = subscriptionDao inTransaction {
    episodeStatusDao.updatePlaybackInfos(playbackInfos)
  }

  //
  // playlist
  //

  override def getPlaylist: Cursor[EpisodeReference] = {
    gdrive.readJsonFile("playlist.json", JsonStreamReader(EpisodeReference, _).toSeqAndClose()) match {
      case Some(playlist) => Cursor(playlist)
      case None => Cursor.empty
    }
  }

  override def putPlaylist(playlist: Seq[EpisodeReference]): Unit =
    gdrive.createOrUpdateJsonFile("playlist.json", JsonWriter(EpisodeReference).writeJson(playlist, _))
}