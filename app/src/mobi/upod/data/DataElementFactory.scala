package mobi.upod.data

trait DataElementFactory {

  def create(name: String, value: Boolean): DataPrimitive

  def create(name: String, value: Double): DataPrimitive

  def create(name: String, value: Float): DataPrimitive

  def create(name: String, value: Int): DataPrimitive

  def create(name: String, value: Long): DataPrimitive

  def create(name: String, value: String): DataPrimitive

  def create(name: String, mapping: (String, DataElement)*): DataObject

  def create(name: String, sequence: Iterable[DataElement]): DataSequence

  def none(name: String): NoData = NoData("")
}
