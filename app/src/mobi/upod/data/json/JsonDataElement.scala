package mobi.upod.data.json

import mobi.upod.data.DataElement
import scala.collection.immutable.ListMap

trait JsonDataElement extends DataElement {

  val parent: Option[JsonDataElement]

  protected def debugInfo: Map[String, Any] = parent.map(p => ListMap("parent" -> p.debugInfo)).getOrElse(Map())
}
