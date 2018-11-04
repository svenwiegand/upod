package mobi.upod.app.services.sync.gdrive

import java.net.URL

import com.escalatesoft.subcut.inject.BindingModule
import mobi.upod.app.services.sync.Subscription
import mobi.upod.app.storage.{Dao, DaoObject, DatabaseHelper, Index}
import mobi.upod.util.Cursor

private[gdrive] class SubscriptionSyncDao(dbHelper: DatabaseHelper)(implicit bindingModule: BindingModule)
  extends Dao[Subscription](SubscriptionSyncDao.table, dbHelper, Subscription)
  with SyncDao[Subscription] {

  import SubscriptionSyncDao._

  override protected def columns: Map[Symbol, String] = Map(
    url -> PRIMARY_KEY_TEXT,
    autoAddEpisodes -> INTEGER,
    autoAddToPlaylist -> INTEGER,
    autoDownload -> INTEGER,
    maxKeptEpisodes -> INTEGER,
    playbackSpeed -> REAL,
    volumeGain -> REAL
  )

  override def exportItems: Cursor[Subscription] =
    findMultiple(sql"SELECT * FROM $table ORDER BY url")

  def deleteSubscriptions(urls: Seq[URL]): Unit = {
    val statement = db.compileStatement(sql"DELETE FROM $table WHERE $url=?")
    urls foreach { url =>
      statement.bindString(1, url.toString)
      statement.executeUpdateDelete()
    }
    statement.close()
  }
}

private[gdrive] object SubscriptionSyncDao extends DaoObject {
  val table = Table('subscription_sync)
  val subscription = table

  val url = Column('url)
  val autoAddEpisodes = Column('settings_autoAddEpisodes)
  val autoAddToPlaylist = Column('settings_autoAddToPlaylist)
  val autoDownload = Column('settings_autoDownload)
  val maxKeptEpisodes = Column('settings_maxKeptEpisodes)
  val playbackSpeed = Column('settings_playbackSpeed)
  val volumeGain = Column('settings_volumeGain)
}