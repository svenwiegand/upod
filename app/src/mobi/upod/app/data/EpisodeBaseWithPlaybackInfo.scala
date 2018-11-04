package mobi.upod.app.data

import mobi.upod.util.MediaPositionFormat.MediaPositionFormat
import mobi.upod.util.{MediaFormat, MediaPosition}
import mobi.upod.util.StorageSize._

trait EpisodeBaseWithPlaybackInfo extends EpisodeBase {
  val playbackInfo: EpisodePlaybackInfo

  def mediaPosition: MediaPosition =
    MediaPosition(playbackInfo.playbackPosition, media.duration)

  def formattedPosition(format: MediaPositionFormat, withSeconds: Boolean): String = {
    if (media.duration > 0)
      MediaFormat.formatFullPosition(mediaPosition, format, withSeconds)
    else if (media.length > 0)
      f"${media.length / 1.mb}%d MB"
    else
      "-:--"
  }
}
