package mobi.upod.app.data

import java.net.URI

import mobi.upod.android.graphics.Color
import mobi.upod.data.{Mapping, MappingProvider}

case class PodcastColorChange(podcast: URI, background: Color, key: Option[Color])

object PodcastColorChange extends MappingProvider[PodcastColorChange] {

  import Mapping._

  override val mapping: Mapping[PodcastColorChange] = map(
    "podcast" -> uri,
    "background" -> int,
    "key" -> optional(int)
  )((uri, background, key) => apply(uri, Color(background), key.map(Color.apply)))(c => Some((c.podcast, c.background.argb, c.key.map(_.argb))))
}
