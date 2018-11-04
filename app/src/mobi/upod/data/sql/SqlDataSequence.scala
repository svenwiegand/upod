package mobi.upod.data.sql

import com.google.gson.{JsonParseException, JsonStreamParser}
import mobi.upod.data.json.JsonDataParser
import mobi.upod.data.{DataElement, DataSequence}
import mobi.upod.sql.Implicits._
import scala.collection.JavaConverters._

private[sql] final class SqlDataSequence private (name: String, str: String) extends DataSequence(name) {
  private val sequence = new JsonDataParser(new JsonStreamParser(str).asScala).next().asInstanceOf[DataSequence]

  def iterator: Iterator[DataElement] = sequence.iterator

  protected def debugInfo = Map("content" -> str)
}

private[sql] object SqlDataSequence {
  def apply(name: String, str: String): Option[SqlDataSequence] = {
    if (!str.isEmpty && str.head == '[' && str.last == ']') {
      try {
        Some(new SqlDataSequence(name, str.unescape))
      } catch {
        case (_: JsonParseException | _: ClassCastException) => None
      }
    } else {
      None
    }
  }
}


