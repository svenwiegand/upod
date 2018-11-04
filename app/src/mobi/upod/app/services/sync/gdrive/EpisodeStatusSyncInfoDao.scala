package mobi.upod.app.services.sync.gdrive

import java.net.{URI, URL}

import android.database.sqlite.SQLiteStatement
import com.escalatesoft.subcut.inject.BindingModule
import mobi.upod.app.data.EpisodeReference
import mobi.upod.app.data.EpisodeStatus.EpisodeStatus
import mobi.upod.app.services.sync.{EpisodePlaybackInfo, EpisodeStatusSyncInfo}
import mobi.upod.app.storage._
import mobi.upod.data.Mapping
import mobi.upod.util.Cursor
import org.joda.time.DateTime

private[gdrive] class EpisodeStatusSyncInfoDao(dbHelper: DatabaseHelper)(implicit bindingModule: BindingModule)
  extends Dao[EpisodeStatusSyncInfo](EpisodeStatusSyncInfoDao.table, dbHelper, EpisodeStatusSyncInfo)
  with SyncDao[EpisodeStatusSyncInfo] {

  import EpisodeStatusSyncInfoDao._

  override protected def columns: Map[Symbol, String] = Map(
    podcast -> TEXT,
    uri -> TEXT,
    status -> TEXT,
    statusModified -> INTEGER,
    playbackPosition -> INTEGER,
    playbackDuration -> INTEGER,
    playbackFinished -> INTEGER,
    playbackSpeed -> REAL,
    playbackGain -> REAL,
    playbackModified -> INTEGER
  )

  override protected def indices: Seq[Index] = Seq(
    Index(table, 'episodeStatusSyncPodcast, false, TextIndexColumn(podcast)),
    Index(table, 'episodeStatusSyncPodcastEpisode, true, TextIndexColumn(podcast), TextIndexColumn(uri)),
    Index(table, 'episodeStatusSyncPodcastStatusModified, false, TextIndexColumn(podcast), IntIndexColumn(statusModified))
  )

  override def exportItems: Cursor[EpisodeStatusSyncInfo] =
    findMultiple(sql"SELECT * FROM $table ORDER BY $podcast, $uri")

  def getPodcastUrls: Cursor[URL] =
    findMultiple(sql"SELECT $podcast FROM $table", Mapping.simpleMapping(podcast.name -> Mapping.url))

  def get(podcast: URL): Cursor[EpisodeStatusSyncInfo] =
    findMultiple(sql"SELECT * FROM $table WHERE ${EpisodeStatusSyncInfoDao.podcast}=$podcast")

  def get(podcast: URL, from: DateTime): Cursor[EpisodeStatusSyncInfo] =
    findMultiple(sql"SELECT * FROM $table WHERE ${EpisodeStatusSyncInfoDao.podcast}=$podcast AND ($statusModified>=$from OR $playbackModified>=$from)")

  def updateStatus(episodes: Iterable[EpisodeReference], status: EpisodeStatus, timestamp: DateTime): Unit = {

    def executeStatement(statement: SQLiteStatement, episode: EpisodeReference): Int = {
      statement.bindString(1, episode.podcast.toString)
      statement.bindString(2, episode.uri.toString)
      statement.executeUpdateDelete()
    }

    val updateStatement = db.compileStatement(sql"""
      UPDATE $table SET
        ${Table.status}=$status,
        $statusModified=$timestamp
      WHERE
        ${Table.podcast}=? AND
        ${Table.uri}=?
    """)
    val insertStatement = db.compileStatement(sql"""
      INSERT INTO $table ($podcast, $uri, ${Table.status}, $statusModified, $playbackPosition, $playbackDuration, $playbackFinished, $playbackModified)
      VALUES (?, ?, $status, $timestamp, 0, 0, 0, 0)
    """)

    episodes foreach { episode =>
      if (executeStatement(updateStatement, episode) < 1)
        executeStatement(insertStatement, episode)
    }

    updateStatement.close()
    insertStatement.close()
  }

  def updatePlaybackInfos(playbackInfos: Iterable[EpisodePlaybackInfo]): Unit = {

    val statement = db.compileStatement(sql"""
      UPDATE $table SET
        $playbackPosition=?,
        $playbackDuration=?,
        $playbackFinished=?,
        $playbackSpeed=?,
        $playbackGain=?,
        $playbackModified=?
      WHERE
        $podcast=? AND
        $uri=?
    """)

    def bindOptionalFloat(index: Int, value: Option[Float]): Unit = value match {
      case Some(v) => statement.bindDouble(index, v)
      case None => statement.bindNull(index)
    }

    playbackInfos foreach { pi =>
      statement.bindLong(1, pi.playbackInfo.position)
      statement.bindLong(2, pi.playbackInfo.duration)
      statement.bindLong(3, if (pi.playbackInfo.finished) 1 else 0)
      bindOptionalFloat(4, pi.playbackInfo.speed)
      bindOptionalFloat(5, pi.playbackInfo.gain)
      statement.bindLong(6, pi.playbackInfo.modified.getMillis)
      statement.bindString(7, pi.podcast.toString)
      statement.bindString(8, pi.uri.toString)
      statement.executeUpdateDelete()
    }
    statement.close()
  }

  def delete(episodes: Iterable[EpisodeReference]): Unit = {
    val statement = db.compileStatement(sql"DELETE FROM $table WHERE $podcast=? AND $uri=?")
    episodes foreach { e =>
      statement.bindString(1, e.podcast.toString)
      statement.bindString(2, e.uri.toString)
      statement.executeUpdateDelete()
    }
    statement.close()
  }

  def deleteByPodcast(podcasts: Iterable[URL]): Unit = {
    val statement = db.compileStatement(sql"DELETE FROM $table WHERE $podcast=?")
    podcasts foreach { p =>
      statement.bindString(1, p.toString)
      statement.executeUpdateDelete()
    }
    statement.close()
  }

  def deleteUnreferenced(): Unit = execUpdateOrDelete(sql"""
    DELETE FROM $table
    WHERE
      $podcast NOT IN (SELECT ${SubscriptionSyncDao.url} FROM ${SubscriptionSyncDao.table}) AND
      $podcast NOT IN (SELECT DISTINCT es.$podcast FROM $table es WHERE es.status IN ('library', 'starred'))
  """)
}

private[gdrive] object EpisodeStatusSyncInfoDao extends DaoObject {
  val Table = this
  override val table: Symbol = Table('episode_status_sync)
  val episodeStatus = table
  val podcast = Column('podcast)
  val uri = Column('uri)
  val status = Column('status)
  val statusModified = Column('statusModified)
  val playbackPosition = Column('playbackInfo_position)
  val playbackDuration = Column('playbackInfo_duration)
  val playbackFinished = Column('playbackInfo_finished)
  val playbackSpeed = Column('playbackInfo_speed)
  val playbackGain = Column('playbackInfo_gain)
  val playbackModified = Column('playbackInfo_modified)
}