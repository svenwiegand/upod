package mobi.upod.net

import java.net.{URLEncoder, URISyntaxException, URI, URL}

object UriUtils {

  def create(uri: Option[String], fallbackUrl: Option[URL]): URI = uri match {
    case Some(uriString) if !uriString.isEmpty => create(uriString, fallbackUrl)
    case _ => fallbackUrl match {
      case Some(url) => createFromUrl(url)
      case None => throw new IllegalArgumentException("no value specified")
    }
  }

  def create(uri: String, fallbackUrl: Option[URL] = None): URI = {
    try {
      new URI(uri)
    } catch {
      case ex: URISyntaxException =>
        try {
          new URI(encode(uri))
        } catch {
          case ex2: URISyntaxException =>
            fallbackUrl match {
              case Some(url) => createFromUrl(url)
              case None => throw ex
            }
        }
    }
  }

  def createFromUrl(url: URL): URI =
    new URI(url.getProtocol, url.getAuthority, url.getPath, url.getQuery, url.getRef)

  def encode(uri: String): String =
    URLEncoder.encode(uri, "ASCII")
}
