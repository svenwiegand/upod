package mobi.upod.app.storage

import java.net.URL

import com.escalatesoft.subcut.inject.BindingModule
import mobi.upod.app.data.SubscriptionChange
import mobi.upod.app.services.sync.Subscription
import mobi.upod.data.Mapping
import mobi.upod.util.Cursor
import org.joda.time.DateTime

class SubscriptionChangeDao(dbHelper: DatabaseHelper)(implicit bindingModule: BindingModule)
  extends Dao[SubscriptionChange](SubscriptionChangeDao.table, dbHelper, SubscriptionChange) {

  import mobi.upod.app.storage.PodcastDao.{podcast, settingsAutoAdd => autoAdd, settingsAutoAddToPlaylist => autoAddToPlaylist, settingsAutoDownload => autoDownload, settingsMaxKeptEpisodes => maxKeptEpisodes, settingsPlaybackSpeed => playbackSpeed, settingsVolumeGain => volumeGain, subscribed => podcastSubscribed, url => podcastUrl}
  import mobi.upod.app.storage.SubscriptionChangeDao._

  protected def columns = Map(
    id -> PRIMARY_KEY,
    url -> TEXT,
    subscribed -> INTEGER,
    timestamp -> INTEGER
  )

  override protected def tableConstraints = Seq(sql"CONSTRAINT uniqueEntry UNIQUE ($url)")

  override protected val triggers = Seq(
    Trigger(
      'logSubscriptions,
      sql"AFTER UPDATE OF $podcastSubscribed ON $podcast FOR EACH ROW WHEN OLD.$podcastSubscribed IS NOT NEW.$podcastSubscribed",
      sql"INSERT OR REPLACE INTO $table ($url, $subscribed, $timestamp) VALUES (NEW.$podcastUrl, NEW.$podcastSubscribed, $NOW)"),
    Trigger(
      'logImportedSubscriptions,
      sql"AFTER INSERT ON ${ImportedSubscriptionsDao.table} FOR EACH ROW",
      sql"INSERT OR REPLACE INTO $table ($url, $subscribed, $timestamp) VALUES (NEW.podcast, $Subscribed, $NOW)"),
    Trigger(
      'logSubscriptionSettings,
      sql"""AFTER UPDATE OF
            $autoAddToPlaylist, $autoAdd, $autoDownload, $maxKeptEpisodes, $playbackSpeed, $volumeGain
            ON $podcast FOR EACH ROW WHEN
            NEW.$autoAddToPlaylist IS NOT OLD.$autoAddToPlaylist OR
            NEW.$autoAdd IS NOT OLD.$autoAdd OR
            NEW.$autoDownload IS NOT OLD.$autoDownload OR
            NEW.$maxKeptEpisodes IS NOT OLD.$maxKeptEpisodes OR
            NEW.$playbackSpeed IS NOT OLD.$playbackSpeed OR
            NEW.$volumeGain IS NOT OLD.$volumeGain""",
      sql"INSERT OR REPLACE INTO $table ($url, $subscribed, $timestamp) VALUES (NEW.$podcastUrl, $Subscribed, $NOW)"),
    Trigger(
      'logPodcastDeletion,
      sql"AFTER DELETE ON $podcast FOR EACH ROW",
      sql"INSERT OR REPLACE INTO $table ($url, $subscribed, $timestamp) VALUES (OLD.$podcastUrl, $Deleted, $NOW)")
  )

  override protected[storage] def upgrade(database: Database, oldVersion: Int, newVersion: Int) {
    log.info(s"upgrading subscriptionChange table from $oldVersion to $newVersion")
    if (oldVersion < 94) {
      recreate(database)
    }
    if (oldVersion < 97) {
      recreateTriggers(database)
    }
  }

  def replaceAllWithCurrentSubscriptions(): Unit = {
    execSql(sql"DELETE FROM $table")
    execSql(sql"INSERT INTO $table ($url, $subscribed, $timestamp) SELECT $podcastUrl, $Subscribed, $NOW FROM $podcast WHERE $podcastSubscribed=1")
  }

  def findSubscribedAndChanged(until: DateTime): Cursor[Subscription] = {
    findMultiple(sql"""
      SELECT
        s.$url url,
        $autoAddToPlaylist,
        $autoAdd,
        $autoDownload,
        $maxKeptEpisodes,
        $playbackSpeed,
        $volumeGain
      FROM $table s INNER JOIN $podcast p on s.$url=p.$podcastUrl
      WHERE s.$subscribed=$Subscribed AND s.$timestamp <= $until""",
      Subscription)
  }

  def findUnsubscribed(until: DateTime): Cursor[URL] = {
    findMultiple(sql"SELECT $url FROM $table WHERE ($subscribed=$Unsubscribed OR $subscribed=$Deleted) AND $timestamp <= $until",
      Mapping.simpleMapping("url" -> Mapping.url))
  }

  def findDeletedPodcasts(until: DateTime): Cursor[URL] = {
    findMultiple(sql"SELECT $url FROM $table WHERE $subscribed=$Deleted AND $timestamp <= $until",
      Mapping.simpleMapping("url" -> Mapping.url))
  }

  def deleteUntil(until: DateTime) =
    deleteWhere(sql"$timestamp <= $until")
}

object SubscriptionChangeDao extends DaoObject {
  val Subscribed = 1
  val Unsubscribed = 0
  val Deleted = -1

  val table = Table('subscriptionChange)

  val id = Column('id)
  val url = Column('url)
  val subscribed = Column('subscribed)
  val timestamp = Column('timestamp)
}
