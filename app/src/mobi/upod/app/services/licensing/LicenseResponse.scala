package mobi.upod.app.services.licensing

private[services] case class LicenseResponse(
  rawData: String,
  signature: String,
  license: License
)
