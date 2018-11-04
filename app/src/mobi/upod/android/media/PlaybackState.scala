package mobi.upod.android.media

sealed trait PlaybackState

sealed trait PlaybackStateWithPosition extends PlaybackState {
  val position: Long
}

object PlaybackState {
  object Connecting extends PlaybackState
  object Buffering extends PlaybackState
  final case class Playing(position: Long, speed: Float) extends PlaybackStateWithPosition
  final case class Paused(position: Long) extends PlaybackStateWithPosition
  object Stopped extends PlaybackState
}
