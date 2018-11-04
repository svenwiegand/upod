package mobi.upod.app.services.sync

/** A `SyncListener` is informed about a sync start and end */
trait SyncListener {

  /** Called when a sync is started */
  def onSyncStarted() {}

  /** Called when a sync finished -- no matter whether it finished successful or with an error */
  def onSyncFinished() {}
}
