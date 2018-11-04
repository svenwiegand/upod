package mobi.upod.android.app.permission

import android.app.Activity
import android.content.pm.PackageManager
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat

trait PermissionRequestingActivity extends Activity with ActivityCompat.OnRequestPermissionsResultCallback {
  import PermissionRequestingActivity.Callback

  private var permissionRequestCode = 1234
  private var callbackByRequestCode = Map[Int, Callback]()

  def ensureHasPermission(permission: String, onResult: Callback): Unit = {
    if (hasPermission(permission))
      onResult(PackageManager.PERMISSION_GRANTED)
    else
      requestPermission(permission, onResult)
  }

  def checkPermission(permission: String): Int =
    ContextCompat.checkSelfPermission(this, permission)

  def hasPermission(permission: String): Boolean =
    checkPermission(permission) == PackageManager.PERMISSION_GRANTED

  def requestPermission(permission: String, onResult: Callback): Unit = {
    callbackByRequestCode += permissionRequestCode -> onResult
    ActivityCompat.requestPermissions(this, Array(permission), permissionRequestCode)
    permissionRequestCode += 1
  }

  override def onRequestPermissionsResult(requestCode: Int, permissions: Array[String], grantResults: Array[Int]): Unit = {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)

    callbackByRequestCode.get(requestCode) match {
      case Some(callback) if grantResults.length > 0 => callback(grantResults(0))
      case _ =>
    }
    callbackByRequestCode -= requestCode
  }
}

object PermissionRequestingActivity {
  type Callback = Int => Unit
}