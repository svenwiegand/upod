package mobi.upod.app.gui.sync

import android.app.PendingIntent
import android.content.{Context, Intent}
import android.os.Bundle
import mobi.upod.android.view.wizard._
import mobi.upod.android.widget.Toast
import mobi.upod.app.gui.MainActivity
import mobi.upod.app.services.sync.{SyncConflictResolution, SyncService}
import mobi.upod.app.{App, AppInjection, R}

class SyncConflictActivity extends WizardActivity with AppInjection {

  import SyncConflictActivity._

  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)
    app.cancelErrorNotification(NotificationTag)
  }

  override protected def hasNextPage(currentPageIndex: Int, currentPageKey: String): Boolean = currentPageKey match {
    case _ => false
  }

  override protected def createFirstPage: WizardPage =
    new SyncConflictPage

  override protected def createNextPage(currentPageIndex: Int, currentPageKey: String): WizardPage =
    throw new UnsupportedOperationException("Only a one pager")

  protected def onFinish(): Unit =
    SyncConflictActivity.onFinish()

  override protected val followUpActivity =
    classOf[MainActivity]
}


object SyncConflictActivity extends AppInjection {
  private val PageKeySyncConflict = "syncConflict"
  private var syncConflictResolution: Option[SyncConflictResolution.Value] = None
  private val NotificationTag = "uPodOutOfSync"

  private def intent(context: Context): Intent =
    new Intent(context, classOf[SyncConflictActivity])

  private def pendingIntent: PendingIntent =
    PendingIntent.getActivity(app, 0, intent(app), Intent.FLAG_ACTIVITY_NEW_TASK)

  def showNotification(): Unit =
    app.notifyError(NotificationTag, app.getString(R.string.sync_conflict_title), app.getString(R.string.sync_conflict_content), Some(SyncConflictActivity.pendingIntent))

  class SyncConflictPage extends SimpleSingleChoicePage[SyncConflictResolution.Value](
    PageKeySyncConflict,
    R.string.wizard_sync_conflict,
    R.string.wizard_sync_conflict_introduction,
    0,
    None,
    choice => syncConflictResolution = Some(choice),
    ValueChoice(SyncConflictResolution.UseServerState, R.string.wizard_sync_conflict_use_server_state),
    ValueChoice(SyncConflictResolution.UseDeviceState, R.string.wizard_sync_conflict_use_device_state)
  )

  private def onFinish(): Unit = {
    inject[SyncService].requestFullSync(true, syncConflictResolution)
    Toast.show(inject[App], R.string.sync_started)
  }
}
