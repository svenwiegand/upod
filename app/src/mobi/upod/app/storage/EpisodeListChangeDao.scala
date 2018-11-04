package mobi.upod.app.storage

import com.escalatesoft.subcut.inject.BindingModule
import mobi.upod.app.data.{EpisodeListChange, EpisodeReference}
import mobi.upod.data.{Mapping, MappingProvider}
import mobi.upod.sql.Sql
import org.joda.time.DateTime

class EpisodeListChangeDao(dbHelper: DatabaseHelper)(implicit bindingModule: BindingModule)
  extends Dao[EpisodeListChange](EpisodeListChangeDao.table, dbHelper, EpisodeListChange) {

  import mobi.upod.app.data.EpisodeStatus._
  import mobi.upod.app.storage.EpisodeDao.{episode, isNew, library, playbackFinished, podcastUrl => episodePodcastUrl, starred, uri => episodeUri}
  import mobi.upod.app.storage.EpisodeListChangeDao._

  protected def columns = Map(
    id -> PRIMARY_KEY,
    podcast -> TEXT,
    uri -> TEXT,
    status -> TEXT,
    timestamp -> INTEGER
  )

  override protected def tableConstraints = Seq(sql"CONSTRAINT uniqueEntry UNIQUE ($podcast, $uri)")

  private def EpisodeStatusChangeTrigger(changedColumn: Symbol, newStatus: EpisodeStatus, additionalWhen: Option[Sql] = None) = {
    val additionalWhenCondition = additionalWhen match {
      case Some(condition) => sql"AND $condition"
      case None => sql""
    }
    new Trigger(
      Symbol("log" + newStatus.toString + "Episodes"),
      sql"AFTER UPDATE OF $changedColumn ON $episode FOR EACH ROW WHEN NEW.$changedColumn<>0 AND NEW.$changedColumn IS NOT OLD.$changedColumn $additionalWhenCondition",
      sql"INSERT OR REPLACE INTO $table ($podcast, $uri, $status, $timestamp) VALUES (NEW.$episodePodcastUrl, NEW.$episodeUri, $newStatus, $NOW)"
    )
  }

  private def EpisodeUnstarTrigger(condition: Sql, newStatus: EpisodeStatus) = Trigger(
    Symbol("logUnstar_" + newStatus.toString + "_Episodes"),
    sql"AFTER UPDATE OF $starred ON $episode FOR EACH ROW WHEN NEW.$starred=0 AND NEW.$starred IS NOT OLD.$starred AND $condition",
    sql"INSERT OR REPLACE INTO $table ($podcast, $uri, $status, $timestamp) VALUES (NEW.$episodePodcastUrl, NEW.$episodeUri, $newStatus, $NOW)"
  )

  override protected val triggers = Seq(
    EpisodeStatusChangeTrigger(isNew, New),
    EpisodeStatusChangeTrigger(library, Library, Some(sql"NEW.$starred=0")),
    EpisodeStatusChangeTrigger(starred, Starred),
    EpisodeStatusChangeTrigger(playbackFinished, Finished, Some(sql"OLD.$starred=0")),
    EpisodeUnstarTrigger(sql"NEW.$playbackFinished<>0", Finished),
    EpisodeUnstarTrigger(sql"NEW.$playbackFinished=0", Library),
    Trigger(
      'logNoLongerAvailableEpisodes,
      sql"AFTER DELETE ON $episode FOR EACH ROW",
      sql"INSERT OR REPLACE INTO $table ($podcast, $uri, $status, $timestamp) VALUES (OLD.$episodePodcastUrl, OLD.$episodeUri, $NoLongerAvailable, $NOW)")
  )

  override protected[storage] def upgrade(database: Database, oldVersion: Int, newVersion: Int): Unit = {
    if (oldVersion < 65) {
      execSql(database, sql"DROP TRIGGER IF EXISTS logInboxRemovals")
      execSql(database, sql"DROP TRIGGER IF EXISTS logLibraryRemovals")
      execSql(database, sql"DROP TRIGGER IF EXISTS logMoveToLibrary")
      execSql(database, sql"DROP TRIGGER IF EXISTS logAddToLibrary")
      execSql(database, sql"DROP TRIGGER IF EXISTS logStar")
      execSql(database, sql"DROP TRIGGER IF EXISTS logUnstar")
      recreate(database)
    }
    if (oldVersion < 78) {
      recreateTriggers(database)
    }
  }

  def replaceAllWithCurrentEpisodeStatus(): Unit = {
    execSql(sql"DELETE FROM $table")
    execSql(sql"INSERT OR REPLACE INTO $table ($podcast, $uri, $status, $timestamp) SELECT $episodePodcastUrl, $episodeUri, $New, $NOW FROM $episode WHERE $isNew=1")
    execSql(sql"INSERT OR REPLACE INTO $table ($podcast, $uri, $status, $timestamp) SELECT $episodePodcastUrl, $episodeUri, $Library, $NOW FROM $episode WHERE $library=1")
    execSql(sql"INSERT OR REPLACE INTO $table ($podcast, $uri, $status, $timestamp) SELECT $episodePodcastUrl, $episodeUri, $Finished, $NOW FROM $episode WHERE $playbackFinished=1")
    execSql(sql"INSERT OR REPLACE INTO $table ($podcast, $uri, $status, $timestamp) SELECT $episodePodcastUrl, $episodeUri, $Starred, $NOW FROM $episode WHERE $starred=1")
  }

  def findCount(status: EpisodeStatus, until: DateTime): Int = findOne(
    sql"SELECT COUNT($id) num FROM $table WHERE ${EpisodeListChangeDao.status}=$status AND $timestamp<=$until",
    Mapping.simpleMapping("num" -> Mapping.int)
  ).getOrElse(0)

  /** Finds a limited amount of episodes having changed to the specified status before the specified time.
    *
    * The number of the returned items is restricted. You should delete the found items and call the method again to
    * find the next batch.
    *
    * @param status the status to search for
    * @param until the time until (including) until which to search for
    * @param limit number of results to find
    * @return references to the found episodes and the hightes row ID.
    */
  def findLimited(status: EpisodeStatus, until: DateTime, limit: Int): (Seq[EpisodeReference], Long) = {
    val result = findMultiple(
      sql"SELECT $id, $podcast, $uri FROM $table WHERE ${EpisodeListChangeDao.status}=$status AND $timestamp<=$until ORDER BY $id LIMIT $limit",
      EpisodeReferenceWithId.mapping).toSeqAndClose()
    val maxRowId = if (result.nonEmpty) result.map(_._1).max else 0
    (result.map(_._2), maxRowId)
  }


  def delete(status: EpisodeStatus, untilRowId: Long): Unit =
    execSql(sql"DELETE FROM $table WHERE $id<=$untilRowId AND ${EpisodeListChangeDao.status}=$status")

  def deleteUntil(until: DateTime): Unit =
    execSql(sql"DELETE FROM $table WHERE $timestamp<=$until")

  def deleteWithoutPodcastInfo(): Unit =
    execSql(sql"DELETE FROM $table WHERE $podcast IS NULL")
}

object EpisodeListChangeDao extends DaoObject {
  val table = Table('episodeListChange)

  val id = Column('id)
  val podcast = Column('podcast)
  val uri = Column('uri)
  val status = Column('status)
  val timestamp = Column('timestamp)

  private object EpisodeReferenceWithId extends MappingProvider[(Long, EpisodeReference)] {

    override val mapping: Mapping[(Long, EpisodeReference)] = Mapping.map(
      "id" -> Mapping.long,
      "podcast" -> Mapping.url,
      "uri" -> Mapping.uri
    )(_ -> EpisodeReference(_, _))(e => Some((e._1, e._2.podcast, e._2.uri)))
  }
}
