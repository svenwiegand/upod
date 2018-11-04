package mobi.upod.data

class NoData(name: String) extends DataElement(name) {
  protected def debugInfo: Map[String, Any] = Map()
}

object NoData {
  def apply(name: String) = new NoData(name)
}
