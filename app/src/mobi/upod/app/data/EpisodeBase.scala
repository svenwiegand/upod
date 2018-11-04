package mobi.upod.app.data

import java.net.URI

import org.joda.time.DateTime

trait EpisodeBase {
  def id: Long
  val uri: URI
  val published: DateTime
  val title: String
  val link: Option[String]
  val media: Media
  val podcast: URI
  val podcastInfo: EpisodePodcastInfo
  val isNew: Boolean
  val library: Boolean

  lazy val episodeId = EpisodeId(podcast, uri)

  def isVideo: Boolean =
    media.mimeType.isVideo

  def isAudio: Boolean =
    media.mimeType.isAudio

  def showNotesLink: Option[String] = link match {
    case Some(url) if url != media.url.toString => Some(url)
    case _ => None
  }

  def extractedOrGeneratedColors: PodcastColors =
    podcastInfo.colors getOrElse PodcastColors.forAny(podcast.toString)
}
