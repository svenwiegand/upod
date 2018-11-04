package mobi.upod.app.storage

import android.content.Context
import android.database.sqlite.{SQLiteDatabase, SQLiteOpenHelper}
import com.escalatesoft.subcut.inject.BindingModule
import mobi.upod.android.logging.Logging

final class DatabaseHelper private (context: Context, name: String, version: Int)(implicit val bindingModule: BindingModule)
    extends SQLiteOpenHelper(context, name, null, version)
    with Logging {

  lazy val readable = new Database(getReadableDatabase, this)
  lazy val writable = new Database(getWritableDatabase, this)

  lazy val podcastDao = new PodcastDao(this)
  lazy val episodeDao = new EpisodeDao(this)
  lazy val podcastColorChangeDao = new PodcastColorChangeDao(this)
  lazy val episodeListChangeDao = new EpisodeListChangeDao(this)
  lazy val importedSubscriptionsDao = new ImportedSubscriptionsDao(this)
  lazy val subscriptionChangeDao = new SubscriptionChangeDao(this)
  lazy val announcementDao = new AnnouncementDao(this)
  private lazy val daos = Seq(
    podcastDao,
    episodeDao,
    podcastColorChangeDao,
    episodeListChangeDao,
    importedSubscriptionsDao,
    subscriptionChangeDao,
    announcementDao
  )

  def onCreate(db: SQLiteDatabase) {
    log.info("creating database")
    val database = new Database(db, this)
    database.inTransaction {
      daos.foreach(_.create(database))
    }
  }

  private def recreate(db: SQLiteDatabase) {
    val database = new Database(db, this)
    database.newTransaction {
      daos.reverse.foreach(_.drop(database))
      onCreate(db)
    }
  }

  def onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    log.crashLogInfo(s"upgrading database from $oldVersion to $newVersion")
    if (oldVersion < 18) {
      recreate(db)
    } else {
      val database = new Database(db, this)
      database.newTransaction {
        daos.foreach(_.upgrade(database, oldVersion, newVersion))
      }
    }
  }

  def enableTriggers() {
    daos.foreach(_.enableTriggers())
  }

  def disableTriggers() {
    daos.foreach(_.disableTriggers())
  }
}

object DatabaseHelper {
  val name = "upod"
  val schemaVersion = 99

  def apply(context: Context)(implicit bindingModule: BindingModule): DatabaseHelper =
    new DatabaseHelper(context, name, schemaVersion)
}

