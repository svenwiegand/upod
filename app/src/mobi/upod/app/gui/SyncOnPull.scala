package mobi.upod.app.gui

import com.escalatesoft.subcut.inject.Injectable
import mobi.upod.android.app.SwipeRefresh
import mobi.upod.android.logging.Logging
import mobi.upod.app.services.sync.{SyncListener, SyncService}

trait SyncOnPull extends SwipeRefresh with SyncListener with Injectable with Logging {
  protected lazy val syncService = inject[SyncService]

  override def onStart(): Unit = {
    super.onStart()
    syncService.addWeakListener(this, false)
    setRefreshing(syncService.running)
  }

  override def onStop(): Unit = {
    syncService.removeListener(this)
    super.onStop()
  }

  override def onRefresh(): Unit =
    syncService.requestFullSync(true)

  override def onSyncStarted(): Unit = {
    super.onSyncStarted()
    setRefreshing(true)
  }

  override def onSyncFinished(): Unit = {
    super.onSyncFinished()
    setRefreshing(false)
  }
}
