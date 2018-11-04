package mobi.upod.app.services.sync

import java.net.{URI, URL}

import mobi.upod.app.data.{EpisodeReference, PlaybackInfo}
import mobi.upod.data.MappingProvider

case class EpisodePlaybackInfo(podcast: URL, uri: URI, playbackInfo: PlaybackInfo) {

  lazy val ref = EpisodeReference(podcast, uri)

  override def hashCode() = ref.hashCode()
}

object EpisodePlaybackInfo extends MappingProvider[EpisodePlaybackInfo] {

  import mobi.upod.data.Mapping._

  val mapping = map(
    "podcast" -> url,
    "uri" -> uri,
    "playbackInfo" -> PlaybackInfo.mapping
  )((podcast, uri, playbackInfo) => EpisodePlaybackInfo(podcast, uri, playbackInfo))(e => Some((e.podcast, e.uri, e.playbackInfo)))

  val jsonMapping = map(
    "podcast" -> url,
    "uri" -> uri,
    "position" -> long,
    "duration" -> long,
    "finished" -> boolean,
    "speed" -> optional(float),
    "gain" -> optional(float),
    "modified" -> dateTime
  ) { (podcast, uri, position, duration, finished, speed, gain, modified) =>
    EpisodePlaybackInfo(podcast, uri, PlaybackInfo(position, duration, finished, speed, gain, modified))
  } { e =>
    Some((e.podcast, e.uri, e.playbackInfo.position, e.playbackInfo.duration, e.playbackInfo.finished, e.playbackInfo.speed, e.playbackInfo.gain, e.playbackInfo.modified))
  }
}
