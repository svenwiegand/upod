package mobi.upod.app.data

import java.net.{URI, URL}

trait PodcastBase {
  def id: Long
  val uri: URI
  val url: URL
  val title: String
  val categories: Set[Category]
  val imageUrl: Option[URL]
  val colors: Option[PodcastColors]
  val subscribed: Boolean
  val syncError: Option[String]

  def extractedOrGeneratedColors: PodcastColors =
    colors getOrElse PodcastColors.forAny(uri.toString)
}
