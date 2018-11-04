package mobi.upod.app.storage

object AutoDownloadStrategy extends Enumeration {
  type AutoDownloadStrategy = Value
  val Manual = Value("Manual")
  val NonMeteredConnection = Value("NonMeteredConnection")
  val AnyConnection = Value("AnyConnection")
}
