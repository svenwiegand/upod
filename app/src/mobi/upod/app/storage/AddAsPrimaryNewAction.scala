package mobi.upod.app.storage

object AddAsPrimaryNewAction extends Enumeration {
  type AddAsPrimaryNewAction = Value
  val Never = Value("Never")
  val NotDownloaded = Value("NotDownloaded")
  val Always = Value("Always")
}
