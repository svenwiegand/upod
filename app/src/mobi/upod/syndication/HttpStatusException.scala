package mobi.upod.syndication

import java.net.HttpURLConnection._

import com.google.code.rome.android.repackaged.com.sun.syndication.fetcher.FetcherException

class HttpStatusException(val status: Int, message: String) extends FetcherException(s"$status: $message")

class HttpClientException(status: Int, message: String) extends HttpStatusException(status, message)

class HttpForbiddenException extends HttpClientException(HTTP_FORBIDDEN, "Authentication required")

class HttpNotFoundException extends HttpClientException(HTTP_NOT_FOUND, "Not found")

class HttpGoneException extends HttpClientException(HTTP_GONE, "Gone")

class HttpServerException(status: Int, message: String) extends HttpStatusException(status, message)

object HttpStatusException {

  def apply(status: Int): Unit = status match {
    case HTTP_FORBIDDEN => throw new HttpForbiddenException
    case HTTP_NOT_FOUND => throw new HttpNotFoundException
    case HTTP_GONE => throw new HttpGoneException
    case s if s >= 400 && s < 500 => throw new HttpClientException(s, "Failed to retrieve resource")
    case s if s >= 500 && s < 600 => throw new HttpServerException(s, "Failed to retrieve resource")
    case _ =>
  }
}