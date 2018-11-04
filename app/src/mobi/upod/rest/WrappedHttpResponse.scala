package mobi.upod.rest

import com.google.api.client.http.HttpResponse
import java.io.{BufferedReader, InputStreamReader}
import mobi.upod.data.Mapping
import mobi.upod.data.json.{JsonStreamReader, JsonReader}
import mobi.upod.io._

class WrappedHttpResponse(val response: HttpResponse) extends AnyVal {

  def as[A](mapping: Mapping[A]): A =
    JsonReader(mapping).readObject(response.getContent)

  def asStreamOf[A](mapping: Mapping[A]): JsonStreamReader[A] =
    JsonStreamReader(mapping, response.getContent)

  def asString: String = {
    def readAll(str: StringBuilder, reader: BufferedReader): StringBuilder = {
      val line = reader.readLine()
      if (line != null) {
        str ++= line
        readAll(str, reader)
      } else {
        str
      }
    }

    forCloseable(new BufferedReader(new InputStreamReader(response.getContent, response.getContentCharset))) { reader =>
      readAll(new StringBuilder, reader).toString()
    }
  }

  def asInt: Int = {
    val string = asString
    string.toInt
  }
}

object WrappedHttpResponse {

  def apply(response: HttpResponse) = new WrappedHttpResponse(response)

  def wrappedResponseToResponse(response: WrappedHttpResponse): HttpResponse = response.response
}
