package mobi.upod.app.services.playback

final case class PlaybackError(reason: PlaybackError.Reason, what: Int = 0, extra: Int = 0) extends Serializable

object PlaybackError extends Enumeration {
  type Reason = Value
  val StorageNotAvailable, FileDoesNotExist, UnsupportedFormat, RemoteError, Unknown = Value
}
