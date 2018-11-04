package mobi.upod.data

abstract class DataObject(name: String) extends DataElement(name) {

  def apply(name: String): DataElement
}
