package mobi.upod.app.services.sync.gdrive

import com.escalatesoft.subcut.inject.BindingModule
import mobi.upod.app.services.device.DeviceIdService
import mobi.upod.data.{Mapping, MappingProvider}
import org.joda.time.DateTime

case class SyncTriggerInfo(
  deviceId: String,
  timestamp: DateTime
)

object SyncTriggerInfo extends MappingProvider[SyncTriggerInfo] {

  import mobi.upod.data.Mapping._

  def apply()(implicit bindings: BindingModule): SyncTriggerInfo =
    apply(bindings.inject[DeviceIdService](None).getDeviceId, new DateTime())

  override val mapping: Mapping[SyncTriggerInfo] = map(
    "deviceId" -> string,
    "timestamp" -> dateTime
  )(apply)(unapply)
}