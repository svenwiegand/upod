package mobi.upod.rest

import java.net.SocketTimeoutException

import com.google.api.client.http._
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.gson.JsonElement
import mobi.upod.android.logging.Logging
import mobi.upod.data.Mapping
import mobi.upod.data.json.JsonWriter
import mobi.upod.util.Duration.IntDuration

abstract class WebService extends Logging {
  import WebService._

  protected val baseUrl: String

  def urlFor(path: String) = new GenericUrl(s"$baseUrl/$path")

  def get(path: String, headers: (String, AnyRef)*): WrappedHttpResponse =
    prepareAndExecuteRequest(requestFactory.buildGetRequest(urlFor(path)).withHeaders(headers: _*))

  def post(path: String, content: HttpContent, headers: (String, AnyRef)*): WrappedHttpResponse =
    prepareAndExecuteRequest(requestFactory.buildPostRequest(urlFor(path), content).withHeaders(headers: _*))

  def post(path: String): WrappedHttpResponse =
    post(path, new EmptyContent)

  def post(path: String, data: JsonElement, headers: (String, AnyRef)*): WrappedHttpResponse = {
    val json = data.toString
    post(path, ByteArrayContent.fromString("application/json", json), headers: _*)
  }

  def post[A](path: String, data: A, mapping: Mapping[A], headers: (String, AnyRef)*): WrappedHttpResponse =
    post(path, JsonWriter(mapping).writeJson(data), headers: _*)

  def put(path: String, content: HttpContent): WrappedHttpResponse =
    prepareAndExecuteRequest(requestFactory.buildPutRequest(urlFor(path), content))

  def put(path: String): WrappedHttpResponse =
    put(path, new EmptyContent)

  def put(path: String, data: JsonElement): WrappedHttpResponse = {
    val json = data.toString
    put(path, ByteArrayContent.fromString("application/json", json))
  }

  def put[A](path: String, data: A, mapping: Mapping[A]): WrappedHttpResponse =
    put(path, JsonWriter(mapping).writeJson(data))

  def put(path: String, data: String): WrappedHttpResponse =
    put(path, ByteArrayContent.fromString("text/plain", data))

  def delete(path: String): WrappedHttpResponse =
    delete(path, new EmptyContent)

  def delete(path: String, data: JsonElement): WrappedHttpResponse = {
    val json = data.toString
    delete(path, ByteArrayContent.fromString("application/json", json))
  }

  def delete[A](path: String, data: A, mapping: Mapping[A]): WrappedHttpResponse =
    delete(path, JsonWriter(mapping).writeJson(data))

  def delete(path: String, content: HttpContent): WrappedHttpResponse =
    prepareAndExecuteRequest(requestFactory.buildRequest(HttpMethods.DELETE, urlFor(path), content))

  protected def prepareAndExecuteRequest(request: HttpRequest): WrappedHttpResponse = {
    prepareRequest(request)
    executeRequest(request)
  }

  protected def prepareRequest(request: HttpRequest) {
    // Fixing EOFException
    // (see https://code.google.com/p/google-http-java-client/issues/detail?id=213 and
    // http://stackoverflow.com/questions/15411213/android-httpsurlconnection-eofexception)
    request.getHeaders.set("Connection", "close")
  }

  protected def executeRequest(request: HttpRequest, retryAllowed: Boolean = true, timeOutRetry: Int = 3): WrappedHttpResponse = {
    try {
      log.crashLogInfo(s"${request.getRequestMethod} on ${request.getUrl}")
      new WrappedHttpResponse(request.execute())
    } catch {
      case ex: HttpResponseException =>
        if (retryAllowed && shouldRetryRequest(request, ex)) {
          log.crashLogInfo(s"HTTP response exception", ex)
          executeRequest(request, false)
        } else
          throw mapHttpException(ex)
      case ex: SocketTimeoutException =>
        if (timeOutRetry > 0) {
          log.crashLogInfo(s"received socket timeout. $timeOutRetry more retries")
          executeRequest(request, true, timeOutRetry - 1)
        }
        else
          throw ex
    }
  }

  protected def shouldRetryRequest(request: HttpRequest, exception: HttpResponseException): Boolean = false

  protected def mapHttpException(ex: HttpResponseException): Throwable = ex

  protected implicit def toRichException(ex: HttpResponseException): RichHttpResponseException =
    new RichHttpResponseException(ex)
}

object WebService {
  private val ConnectionTimeout = 20.seconds
  private val ReadTimeout = 25.seconds

  val httpTransport = new NetHttpTransport()
  val requestFactory = httpTransport.createRequestFactory(RequestInitializer)

  class RichHttpResponseException(val ex: HttpResponseException) extends AnyVal {

    def statusCode = ex.getStatusCode

    def errorCode: Option[String] = {
      val errorCodeValues = ex.getHeaders.getHeaderStringValues("X-ErrorCode")
      if (!errorCodeValues.isEmpty) Some(errorCodeValues.get(0)) else None
    }
  }

  private implicit class RichRequest(val request: HttpRequest) extends AnyVal {
    import scala.collection.JavaConverters._

    def withHeaders(headers: (String, AnyRef)*): HttpRequest = {
      val mapKeyToValues = headers.groupBy(_._1).map { case (key, values) => key -> values.seq.map(_._2) }
      mapKeyToValues foreach { case (key, values) =>
          request.getHeaders.set(key, values.asJava)
      }
      request
    }
  }

  private object RequestInitializer extends HttpRequestInitializer {
    override def initialize(request: HttpRequest): Unit = {
      request.setConnectTimeout(ConnectionTimeout)
      request.setReadTimeout(ReadTimeout)
    }
  }
}
