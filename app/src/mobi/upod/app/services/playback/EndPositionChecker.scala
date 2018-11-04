package mobi.upod.app.services.playback

import mobi.upod.app.data.EpisodeBaseWithPlaybackInfo
import mobi.upod.util.Duration.IntDuration

object EndPositionChecker {

  private val PositionTolerance = 2.seconds

  def isAtEndPosition(episode: EpisodeBaseWithPlaybackInfo, position: Long): Boolean =
    position + PositionTolerance >= episode.media.duration

  def isAtEndPosition(episode: EpisodeBaseWithPlaybackInfo): Boolean =
    isAtEndPosition(episode, episode.playbackInfo.playbackPosition)
}
