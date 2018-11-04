package mobi.upod.data.json

import com.google.gson._
import mobi.upod.data.DataElement

private[json] trait ElementToJsonConverter {
  def convert(element: DataElement): JsonElement = element match {
    case json: JsonDataPrimitive => json.json
    case json: JsonDataObject => json.json
    case json: JsonDataSequence => json.json
    case _ => JsonNull.INSTANCE
  }
}
