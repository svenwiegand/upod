package mobi.upod.app.storage

object PrimaryDownloadAction extends Enumeration {
  type PrimaryDownloadAction = Value
  val AddToDownloadQueue = Value("AddToDownloadQueue")
  val Download = Value("Download")
  val AddToPlaylist = Value("AddToPlaylist")
  val Stream = Value("Stream")
}
