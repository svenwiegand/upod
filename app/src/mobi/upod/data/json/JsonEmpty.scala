package mobi.upod.data.json

import mobi.upod.data.NoData

private[json] class JsonEmpty(name: String, val parent: Option[JsonDataElement])
  extends NoData(name) with JsonDataElement {

  override protected def debugInfo = super.debugInfo
}