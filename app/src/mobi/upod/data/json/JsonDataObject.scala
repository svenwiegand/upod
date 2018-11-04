package mobi.upod.data
package json

import com.google.gson.JsonObject
import scala.collection.immutable.ListMap

private[json] class JsonDataObject(val json: JsonObject, name: String, val parent: Option[JsonDataElement])
  extends DataObject(name) with JsonDataElement with JsonToElementConverter {

  def apply(name: String): DataElement = convert(json.get(name), Some(s"${this.name}.$name"), Some(this))

  override protected def debugInfo = ListMap("json" -> json) ++ super.debugInfo
}
