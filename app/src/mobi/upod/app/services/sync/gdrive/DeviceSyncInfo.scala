package mobi.upod.app.services.sync.gdrive

import mobi.upod.data.{Mapping, MappingProvider}
import org.joda.time.DateTime

private[gdrive] case class DeviceSyncInfo(lastSync: DateTime)

private[gdrive] object DeviceSyncInfo extends MappingProvider[DeviceSyncInfo] {
  import Mapping._

  override val mapping: Mapping[DeviceSyncInfo] = map(
    "lastSync" -> dateTime
  )(apply)(unapply)
}