package mobi.upod.app.storage

import com.escalatesoft.subcut.inject.BindingModule
import mobi.upod.app.data.PlaylistChange
import mobi.upod.data.Mapping

class PlaylistChangeDao(dbHelper: DatabaseHelper)(implicit bindingModule: BindingModule)
  extends Dao[PlaylistChange](PlaylistChangeDao.table, dbHelper, PlaylistChange) {

  import mobi.upod.app.storage.EpisodeDao.{episode, id => episodeKey, playlistPosition}
  import mobi.upod.app.storage.PlaylistChangeDao._

  private lazy val episodeDao = inject[EpisodeDao]

  override protected def columns = Map(
    episodeId -> PRIMARY_KEY_INT,
    position -> INTEGER
  )

  override protected val triggers = Seq(
    Trigger(
      'logPlaylistChanges,
      sql"AFTER UPDATE OF $playlistPosition ON $episode FOR EACH ROW WHEN NEW.$playlistPosition IS NOT OLD.$playlistPosition",
      sql"INSERT OR REPLACE INTO $table ($episodeId, $position) VALUES (NEW.$episodeKey, NEW.$playlistPosition)")
  )

  def create(): Unit =
    db.inTransaction(create(db))

  def drop(): Unit =
    db.inTransaction(drop(db))

  def replayChanges(): Int = {
    val count = findOne(sql"SELECT COUNT(*) c FROM $table", Mapping.simpleMapping("c" -> Mapping.int)).getOrElse(0)
    execSql(sql"UPDATE $episode SET $playlistPosition=NULL WHERE $episodeKey IN (SELECT $episodeId FROM $table WHERE $position IS NULL)")
    val addedEpisodeIds = findMultiple(
      sql"SELECT $episodeId FROM $table WHERE $position IS NOT NULL ORDER BY $position",
      Mapping.simpleMapping("episodeId" -> Mapping.long)).toSeqAndClose()
    episodeDao.addToPlaylist(addedEpisodeIds)
    count
  }
}

object PlaylistChangeDao extends DaoObject {
  val table = Table('playlistChange)

  val episodeId = Column('episodeId)
  val position = Column('position)
}