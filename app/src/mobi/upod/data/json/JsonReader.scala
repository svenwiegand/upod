package mobi.upod.data.json

import com.google.gson.JsonStreamParser
import java.io.{Reader, StringReader, InputStreamReader, InputStream}
import mobi.upod.data.Mapping
import mobi.upod.io._
import scala.collection.JavaConverters._

class JsonReader[A] private (mapping: Mapping[A]) {

  def readObject(str: String): A = readObject(new StringReader(str))

  def readObject(stream: InputStream): A = closeWhenDone(stream) {
    readObject(new InputStreamReader(stream, Charset.utf8))
  }

  def readObject(reader: Reader): A = closeWhenDone(reader) {
    readObject(new JsonStreamParser(reader))
  }

  private def readObject(parser: JsonStreamParser): A = {
    val element = new JsonDataParser(parser.asScala).next()
    mapping.read(element)
  }
}

object JsonReader {
  def apply[A](mapping: Mapping[A]) = new JsonReader[A](mapping)
}
