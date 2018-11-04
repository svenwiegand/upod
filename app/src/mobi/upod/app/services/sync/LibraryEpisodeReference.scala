package mobi.upod.app.services.sync

import mobi.upod.app.data.{EpisodeId, PlaybackInfo}
import mobi.upod.data.{Mapping, MappingProvider}
import org.joda.time.DateTime

private[sync] case class LibraryEpisodeReference(
  id: EpisodeId,
  modified: DateTime,
  starred: Boolean,
  playbackInfo: PlaybackInfo)

private[sync] object LibraryEpisodeReference extends MappingProvider[LibraryEpisodeReference] {

  import Mapping._

  val mapping = map(
    "id" -> EpisodeId.mapping,
    "modified" -> dateTime,
    "starred" -> boolean,
    "playbackInfo" -> PlaybackInfo.mapping
  )(apply)(unapply)
}

