package mobi.upod.app.services.licensing

object LicenseStatus extends Enumeration {
  type LicenseStatus = Value
  val Unlicensed = Value("Unlicensed")
  val GiveawayLicense = Value("GiveawayLicense")
  val GooglePlayLicense = Value("GooglePlayLicense")
}
