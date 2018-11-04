package mobi.upod.app.gui.preference

import android.content.Context
import mobi.upod.android.app.action.{ActionWaitDialog, AsyncAction}
import mobi.upod.android.logging.Logging
import mobi.upod.android.widget.Toast
import mobi.upod.app.services.auth.{AuthService, SignInClient}
import mobi.upod.app.services.sync.{SyncConflictResolution, SyncService}
import mobi.upod.app.services.sync.gdrive.GDriveClient
import mobi.upod.app.{AppInjection, R}

import scala.util.{Failure, Success, Try}

class FixGDriveAppFolderAction extends AsyncAction[Unit, Try[Unit]] with ActionWaitDialog with AppInjection with Logging {
  override protected val waitDialogMessageId = R.string.wait_please

  override protected def getData(context: Context): Unit = ()

  override protected def processData(context: Context, data: Unit): Try[Unit] = {
    val authService = inject[AuthService]
    authService.getUserEmail match {
      case None => Failure(new IllegalStateException("user not authenticated"))
      case Some(accountName) => Try(GDriveClient.fixAppFolder(context, accountName))
    }
  }

  override protected def postProcessData(context: Context, result: Try[Unit]): Unit = {
    super.postProcessData(context, result)

    result match {
      case Success(_) =>
        log.info("fixing GDrive app folder succeeded")
        Toast.show(context, R.string.pref_fix_gdrive_app_folder_succeeded)
        inject[SyncService].requestFullSync(true, Some(SyncConflictResolution.UseDeviceState))
      case Failure(ex) =>
        log.error("fixing GDrive app folder failed", ex)
        Toast.show(context, R.string.pref_fix_gdrive_app_folder_failed)
    }
  }
}
