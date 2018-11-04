package mobi.upod.data
package json

import com.google.gson.JsonPrimitive
import scala.collection.immutable.ListMap

private[json] class JsonDataPrimitive(val json: JsonPrimitive, name: String, val parent: Option[JsonDataElement])
  extends DataPrimitive(name) with JsonDataElement {

  def asBoolean: Boolean = json.getAsBoolean

  def asDouble: Double = json.getAsDouble

  def asFloat: Float = json.getAsFloat

  def asInt: Int = json.getAsInt

  def asLong: Long = json.getAsLong

  def asString: String = json.getAsString

  override protected def debugInfo = ListMap("json" -> json) ++ super.debugInfo
}
