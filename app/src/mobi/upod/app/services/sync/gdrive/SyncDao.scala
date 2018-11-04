package mobi.upod.app.services.sync.gdrive

import mobi.upod.app.storage.Dao
import mobi.upod.util.Cursor

private[gdrive] trait SyncDao[A] extends Dao[A] {

  def create(): Unit =
    recreate(db)

  def importItems(items: Iterator[A]): Unit =
    items.foreach(insertOrFail)

  def exportItems: Cursor[A]

  def destroy(): Unit =
    drop(db)
}
