package mobi.upod.app.storage

import com.escalatesoft.subcut.inject.BindingModule
import mobi.upod.app.data.PodcastColorChange
import mobi.upod.util.Cursor
import org.joda.time.DateTime

class PodcastColorChangeDao(dbHelper: DatabaseHelper)(implicit bindingModule: BindingModule)
  extends Dao[PodcastColorChange](PodcastColorChangeDao.table, dbHelper, PodcastColorChange) {
  
  import mobi.upod.app.storage.PodcastColorChangeDao._
  import PodcastDao.{podcast => podcastTable, uri => podcastUri, backgroundColor => podcastBgColor, keyColor => podcastKeyColor}
  
  override protected def columns = Map(
    podcast -> PRIMARY_KEY_TEXT,
    backgroundColor -> INTEGER,
    keyColor -> INTEGER,
    timestamp -> INTEGER
  )

  override protected[storage] def upgrade(database: Database, oldVersion: Int, newVersion: Int): Unit = {
    if (oldVersion < 70) {
      recreate(database)
    }
    if (oldVersion < 96) {
      execSql(database, sql"DROP TRIGGER IF EXISTS logPodcastColorChanges")
    }
  }

  def find(until: DateTime): Cursor[PodcastColorChange] =
    findMultiple(sql"SELECT * FROM $table WHERE $timestamp<=$until")

  def delete(until: DateTime): Unit =
    execSql(sql"DELETE FROM $table WHERE $timestamp<=$until")
}

object PodcastColorChangeDao extends DaoObject {
  override val table: Symbol = Table('podcastColorChange)
  
  val podcast = Column('podcast)
  val backgroundColor = Column('background)
  val keyColor = Column('key)
  val timestamp = Column('timestamp)
}