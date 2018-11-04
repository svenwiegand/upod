package mobi.upod.app.services

import java.net.URL
import java.util.Locale

import mobi.upod.app.data._
import mobi.upod.data.{Mapping, MappingProvider}
import mobi.upod.net._
import mobi.upod.rest.WebService
import mobi.upod.util.Cursor

class PodcastDirectoryWebService extends WebService {

  import PodcastDirectoryWebService._

  protected val baseUrl = "https://itunes.apple.com"
  private lazy val country = Locale.getDefault.getCountry

  def Query(params: (String, Any)*): String = {
    val parameter = params.map { case (key, value) => url"$key=$value".url }.mkString("&")
    url"search?media=podcast&entity=podcast&country=$country&limit=$Limit&".url + parameter
  }

  def findPopularPodcasts(page: Int, category: Option[String] = None): Cursor[PodcastListItem] =
    if (page == 0) findPopularPodcasts(category) else Cursor.empty

  private def findPopularPodcasts(category: Option[String]): Cursor[PodcastListItem] = {
    val query = (category, category.flatMap(Category.iTunesIdOf)) match {
      case (Some(_), Some(cid)) => Some(Query("term" -> "podcast", "genreId" -> cid))
      case (None, None) => Some(Query("term" -> "podcast"))
      case _ => None
    }
    query match {
      case Some(q) => (get (q) as SearchResult).cursor
      case None => Cursor.empty
    }
  }

  def findPodcasts(query: String, page: Int): Cursor[PodcastListItem] =
    if (page == 0) findPodcasts(query) else Cursor.empty

  private def findPodcasts(query: String): Cursor[PodcastListItem] =
    (get (Query("term" -> query)) as SearchResult).cursor
}

private object PodcastDirectoryWebService {

  private val Limit = 100

  case class SearchResult(results: Seq[Option[PodcastListItem]]) {
    val cursor = Cursor(results.collect{case Some(p) => p})
  }

  object SearchResult extends MappingProvider[SearchResult] {
    import Mapping._

    override val mapping = map(
      "results" -> seq(
        map(
          "feedUrl" -> optional(url),
          "collectionName" -> string,
          "genreIds" -> set(int),
          "artworkUrl100" -> optional(url),
          "trackCount" -> int
        )(iTunesResultToItem)(_ => throw new UnsupportedOperationException("Cannot convert podcast item to ITunesPodcastSearchResult"))
      )
    )(apply)(unapply)

    private def iTunesResultToItem(feedUrl: Option[URL], title: String, genreIds: Set[Int], imageUrl: Option[URL], episodeCount: Int): Option[PodcastListItem] = {
      feedUrl map { url =>
        val categories = genreIds.map(Category.byITunesId).collect { case Some(c) => c }
        PodcastListItem(0, UriUtils.createFromUrl(url), url, title, categories, imageUrl, None, false, None, 0)
      }
    }
  }
}