package mobi.upod.app.services.licensing

private[licensing] trait LicenseCheckerCallback {

  def onLicensed(): Unit

  def onNotLicensed(): Unit

  def onLicenseCheckFailed(errorCode: Int): Unit
}
