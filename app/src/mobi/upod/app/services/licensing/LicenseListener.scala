package mobi.upod.app.services.licensing

trait LicenseListener {

  def onLicensed(): Unit = {
    onLicenseUpdated(true)
  }

  def onNotLicensed(): Unit = {
    onLicenseUpdated(false)
  }

  def onLicenseUpdated(licensed: Boolean): Unit = {}
}
