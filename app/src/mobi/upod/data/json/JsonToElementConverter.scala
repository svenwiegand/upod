package mobi.upod.data.json

import com.google.gson._
import mobi.upod.data.DataElement

private[json] trait JsonToElementConverter {
  def convert(element: JsonElement, name: Option[String] = None, parent: Option[JsonDataElement] = None): DataElement = element match {
    case json: JsonPrimitive => new JsonDataPrimitive(json, name.getOrElse(""), parent)
    case json: JsonObject => new JsonDataObject(json, name.getOrElse(""), parent)
    case json: JsonArray => new JsonDataSequence(json, name.getOrElse(""), parent)
    case _: JsonNull => new JsonEmpty(name.getOrElse(""), parent)
    case null => new JsonMissing(name.getOrElse(""), parent)
  }
}
