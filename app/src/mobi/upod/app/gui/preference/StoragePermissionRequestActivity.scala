package mobi.upod.app.gui.preference

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.support.v4.app.ActivityCompat
import com.escalatesoft.subcut.inject.Injectable
import mobi.upod.android.app.SimpleAlertDialogFragment
import mobi.upod.android.app.action.Action
import mobi.upod.app.services.storage.StorageService
import mobi.upod.app.storage.{StoragePreferences, StorageProvider}
import mobi.upod.app.{App, R}

trait StoragePermissionRequestActivity
  extends Activity
  with ActivityCompat.OnRequestPermissionsResultCallback
  with Injectable {

  private val ExternalStoragePermission = android.Manifest.permission.WRITE_EXTERNAL_STORAGE
  private val ExternalStoragePermissionRequestCode = 1

  protected val requestPermissionOnStart: Boolean = true

  override def onStart(): Unit = {
    super.onStart()

    if (requestPermissionOnStart) {
      ensureExternalStoragePermission()
    }
  }

  protected def shouldRequestExternalStoragePermission: Boolean =
    inject[StoragePreferences].storageProviderType.get != StorageProvider.Internal

  def ensureExternalStoragePermission(requestPermission: Boolean = shouldRequestExternalStoragePermission): Unit = {
    if (!StorageProvider.hasExternalStoragePermissions(this) && requestPermission) {
      if (ActivityCompat.shouldShowRequestPermissionRationale(this, ExternalStoragePermission))
        showExternalStorageRequestExplanation()
      else
        requestExternalStoragePermission()
    } else {
      onStorageAvailable()
    }
  }

  private def showExternalStorageRequestExplanation(): Unit = SimpleAlertDialogFragment.showFromActivity(
    this,
    "externalStoragePermissionRequestExplanation",
    R.string.permission_request_external_storage,
    getString(R.string.permission_request_external_storage_details),
    neutralButtonTextId = Some(R.string.ok),
    neutralAction = Some(new RequestExternalStoragePermissionAction)
  )

  private[preference] def requestExternalStoragePermission(): Unit = {
    val permissions = Array(ExternalStoragePermission)
    ActivityCompat.requestPermissions(this, permissions, ExternalStoragePermissionRequestCode)
  }

  override def onRequestPermissionsResult(requestCode: Int, permissions: Array[String], grantResults: Array[Int]): Unit = requestCode match {
    case ExternalStoragePermissionRequestCode =>
      if (grantResults.length > 0 && grantResults(0) == PackageManager.PERMISSION_GRANTED) {
        // ensure storage provider is set
        inject[StoragePreferences].storageProvider
        inject[StorageService].onExternalStoragePermissionGranted()
        onStorageAvailable()
      } else {
        askToSwitchToInternalStorage()
      }
    case _ => super.onRequestPermissionsResult(requestCode, permissions, grantResults)
  }

  private def askToSwitchToInternalStorage(): Unit = SimpleAlertDialogFragment.showFromActivity(
    this,
    "externalStoragePermissionRequestRejection",
    R.string.permission_request_external_storage,
    getString(R.string.permission_request_external_storage_rejected),
    positiveButtonTextId = Some(R.string.yes),
    negativeButtonTextId = Some(R.string.no),
    positiveAction = Some(new SwitchToInternalStorageAction),
    negativeAction = Some(new EnsureExternalStoragePermissionAction)
  )

  protected[preference] def onStorageAvailable(): Unit = ()
}

class EnsureExternalStoragePermissionAction extends Action {

  override def onFired(context: Context): Unit = context match {
    case a: StoragePermissionRequestActivity => a.ensureExternalStoragePermission(true)
    case _ => // ignore
  }
}

class RequestExternalStoragePermissionAction extends Action {

  override def onFired(context: Context): Unit = context match {
    case a: StoragePermissionRequestActivity => a.requestExternalStoragePermission()
    case _ => // ignore
  }
}

class SwitchToInternalStorageAction extends Action {

  override def onFired(context: Context): Unit = {
    App.inject[StorageService].swtichTo(StorageProvider.Internal)
    context match {
      case a: StoragePermissionRequestActivity => a.onStorageAvailable()
      case _ => // ignore
    }
  }
}
