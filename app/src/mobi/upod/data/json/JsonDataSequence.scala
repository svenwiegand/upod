package mobi.upod.data.json

import mobi.upod.data.{DataElement, DataSequence}
import com.google.gson.JsonArray
import scala.collection.JavaConverters._
import scala.collection.immutable.ListMap

private[json] class JsonDataSequence(val json: JsonArray, name: String, val parent: Option[JsonDataElement])
  extends DataSequence(name) with JsonDataElement with JsonToElementConverter {

  def iterator: Iterator[DataElement] = json.iterator.asScala.map(convert(_, Some(s"$name[]")))

  override protected def debugInfo = ListMap("json" -> json) ++ super.debugInfo
}
