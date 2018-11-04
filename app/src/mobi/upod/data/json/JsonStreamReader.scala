package mobi.upod.data.json

import com.google.gson.JsonStreamParser
import java.io._
import mobi.upod
import mobi.upod.data.Mapping
import mobi.upod.io.Charset
import mobi.upod.util.Cursor
import scala.collection.JavaConverters._

class JsonStreamReader[A] private (mapping: Mapping[A], reader: Reader) extends Cursor[A] {
  private val stream = {
    val jsonStream = new JsonStreamParser(reader)
    require(jsonStream.hasNext)

    val element = jsonStream.next()
    require(element.isJsonArray, s"expected JSON array but found ${element.toString}")

    new JsonDataParser(element.getAsJsonArray.iterator.asScala)
  }


  def hasNext: Boolean = stream.hasNext

  def next(): A = mapping.read(stream.next())

  def close() {
    reader.close()
  }
}

object JsonStreamReader {

  def apply[A](mapping: Mapping[A], inputStream: InputStream): JsonStreamReader[A] =
    new JsonStreamReader(mapping, new InputStreamReader(inputStream, Charset.utf8))

  def apply[A](mapping: Mapping[A], reader: Reader): JsonStreamReader[A] =
    new JsonStreamReader(mapping, reader)

  def apply[A](mapping: Mapping[A], json: String): JsonStreamReader[A] = {
    val stream = new ByteArrayInputStream(json.getBytes(Charset.utf8))
    apply(mapping, stream)
  }

}