package mobi.upod.data

abstract class DataPrimitive(name: String) extends DataElement(name) {
  def asBoolean: Boolean

  def asDouble: Double

  def asFloat: Float

  def asInt: Int

  def asLong: Long

  def asString: String
}
