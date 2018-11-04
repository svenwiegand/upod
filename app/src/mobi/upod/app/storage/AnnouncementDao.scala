package mobi.upod.app.storage

import android.database.sqlite.SQLiteConstraintException
import com.escalatesoft.subcut.inject.BindingModule
import mobi.upod.app.data.Announcement
import mobi.upod.data.Mapping
import org.joda.time.DateTime

class AnnouncementDao(dbHelper: DatabaseHelper)(implicit bindingModule: BindingModule)
  extends Dao[Announcement](AnnouncementDao.table, dbHelper, Announcement.mapping) {

  import AnnouncementDao._

  override protected def columns: Map[Symbol, String] = Map(
    id -> PRIMARY_KEY_INT,
    modifiedDate -> INTEGER,
    startDate -> INTEGER,
    endDate -> INTEGER,
    freeUsersOnly -> INTEGER,
    primaryUrl -> TEXT,
    secondaryUrl -> TEXT,
    title -> TEXT,
    message -> TEXT,
    primaryButton -> TEXT,
    secondaryButton -> TEXT,
    dismissed -> "INTEGER DEFAULT 0"
  )

  override protected[storage] def upgrade(database: Database, oldVersion: Int, newVersion: Int): Unit = {
    log.info(s"upgrading announcement table from $oldVersion to $newVersion")
    if (oldVersion < 98) {
      create(database)
    }
  }

  def insertOrUpdate(announcement: Announcement): Unit = {
    try insertOrFail(announcement) catch {
      case ex: SQLiteConstraintException =>
        val dismissed = isDismissed(announcement.id)
        save(announcement)
        if (dismissed.contains(true)) {
          setDismissed(announcement.id)
        }
    }
  }

  def deleteOldAnnouncements(): Unit =
    deleteWhere(sql"$endDate < ${DateTime.now}")

  def setDismissed(id: Long): Unit =
    execUpdate(sql"UPDATE $announcement SET $dismissed=1 WHERE ${AnnouncementDao.id}=$id")

  def findNextAnnouncement(includeFreeUsersOnly: Boolean): Option[Announcement] = {
    val now = DateTime.now
    findOne(
      sql"""SELECT *
           FROM $announcement
           WHERE $startDate<=$now AND $endDate>$now AND $dismissed=0 AND ($freeUsersOnly=0 OR $freeUsersOnly=$includeFreeUsersOnly)
           ORDER BY $startDate
           LIMIT 1""")
  }

  private def isDismissed(id: Long): Option[Boolean] = findOne(
    sql"SELECT $dismissed FROM $announcement WHERE ${AnnouncementDao.id}=$id",
    Mapping.simpleMapping(dismissed.name -> Mapping.boolean)
  )
}

object AnnouncementDao extends DaoObject {
  val table = Table('announcement)
  val announcement = table

  val id = Column('id)
  val modifiedDate = Column('modifiedDate)
  val startDate = Column('startDate)
  val endDate = Column('endDate)
  val freeUsersOnly = Column('freeUsersOnly)
  val primaryUrl = Column('primaryUrl)
  val secondaryUrl = Column('secondaryUrl)
  val title = Column('title)
  val message = Column('message)
  val primaryButton = Column('primaryButton)
  val secondaryButton = Column('secondaryButton)
  val dismissed = Column('dismissed)
}