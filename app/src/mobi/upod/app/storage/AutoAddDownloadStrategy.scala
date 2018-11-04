package mobi.upod.app.storage

object AutoAddDownloadStrategy extends Enumeration {
  type AutoAddDownloadStrategy = Value
  val Manual = Value("Manual")
  val Playlist = Value("Playlist")
  val Library = Value("Library")
  val NewAndLibrary = Value("NewAndLibrary")
}
