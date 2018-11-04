package mobi.upod.data.json

import mobi.upod.data._
import com.google.gson.{JsonArray, JsonObject, JsonPrimitive}

private[json] object JsonElementFactory extends DataElementFactory with ElementToJsonConverter {

  def create(name: String, value: Boolean) = new JsonDataPrimitive(new JsonPrimitive(value), name, None)

  def create(name: String, value: Double) = new JsonDataPrimitive(new JsonPrimitive(value), name, None)

  def create(name: String, value: Float) = new JsonDataPrimitive(new JsonPrimitive(value), name, None)

  def create(name: String, value: Int) = new JsonDataPrimitive(new JsonPrimitive(value), name, None)

  def create(name: String, value: Long) = new JsonDataPrimitive(new JsonPrimitive(value), name, None)

  def create(name: String, value: String) = new JsonDataPrimitive(new JsonPrimitive(value), name, None)

  def create(name: String, mapping: (String, DataElement)*): JsonDataObject = {
    val json = new JsonObject
    mapping.foreach {
      m =>
        json.add(m._1, convert(m._2))
    }
    new JsonDataObject(json, name, None)
  }

  def create(name: String, sequence: Iterable[DataElement]): JsonDataSequence = {
    val json = new JsonArray
    sequence.foreach {
      element =>
        json.add(convert(element))
    }
    new JsonDataSequence(json, name, None)
  }

  override def none(name: String): NoData = new JsonEmpty(name, None)
}
