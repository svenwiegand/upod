package mobi.upod.app.services.licensing

import android.content.Context
import com.google.android.vending.licensing.{LicenseChecker => GLicenseChecker, Policy, AESObfuscator, ServerManagedPolicy}
import android.content.pm.PackageManager
import mobi.upod.android.logging.Logging

private[licensing] class GooglePlayLicenseChecker(context: Context, callback: LicenseCheckerCallback)
  extends com.google.android.vending.licensing.LicenseCheckerCallback
  with Logging {

  import GooglePlayLicenseChecker._

  log.trace("creating gp")

  private var _licenseChecker: Option[GLicenseChecker] = None

  private def licenseChecker: GLicenseChecker = _licenseChecker match {
    case Some(lc) => lc
    case None =>
      val lc = new GLicenseChecker(
        context,
        new ServerManagedPolicy(context, new AESObfuscator(context)),
        GooglePlayLicense.PackageName,
        PublicKey)
      _licenseChecker = Some(lc)
      lc
  }

  def isLicenseModuleInstalled: Boolean =
      context.getPackageManager.checkSignatures(context.getPackageName, GooglePlayLicense.PackageName) >= PackageManager.SIGNATURE_MATCH

  def checkLicense(): Unit = {
    if (isLicenseModuleInstalled) {
      log.info("license module installed -- initiating google play license check")
      licenseChecker.checkAccess(this)
    } else {
      log.info("license module isn't installed")
      callback.onNotLicensed()
    }
  }

  def destroy(): Unit = {
    _licenseChecker.foreach(_.onDestroy())
  }

  def allow(reason: Int) = {
    log.info(s"license OK ($reason)")
    callback.onLicensed()
  }

  def dontAllow(reason: Int) = reason match {
    case Policy.LICENSED =>
      log.info(s"dontAllow received licensed")
      allow(reason)
    case Policy.RETRY =>
      log.info(s"donAllow received retry")
      applicationError(reason)
    case _ =>
      log.info(s"no license ($reason)")
      callback.onNotLicensed()
  }

  def applicationError(errorCode: Int) = {
    log.warn(s"license check failed ($errorCode)")
    callback.onLicenseCheckFailed(errorCode)
  }
}

private object GooglePlayLicenseChecker {
  val PublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAvVrYRnBHBbT66ymUae4l3VU6fCoVRu5eGjJJjG3N9uEwNzZq5fcuVT/r8OyZlxr76+efcwatQddtmjEnL/VbokFAT9c6gjW3ujOWFCNojRKhAi7NNZ/+RBTO2bRlJrTiBvo8nRbX5TyDObgW7a7vOCuWydiugUkSl07lrz33BkgUQxHz4W6MDaftqBUejDxSL7tGQEKQ/ZFjjEaPX5qMMwE0+shr5NFte9dXtLV1Bo0z25MBSB2gxC5BYT01Y/OFiZ00Pb+E8sRNj0NjkttA0i7WmLFEDYic3DE6QQyNAyoVIYabqdxAuXDiZX3rDsbHACPeuBkS6Lg1qajYSYZNSwIDAQAB"
}
