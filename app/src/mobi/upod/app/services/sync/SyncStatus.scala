package mobi.upod.app.services.sync

import com.escalatesoft.subcut.inject.BindingModule
import com.github.nscala_time.time.Imports._
import mobi.upod.android.logging.Logging
import mobi.upod.app.services.device.DeviceIdService
import mobi.upod.app.storage.InternalSyncPreferences

sealed trait SyncStatus
object InSync extends SyncStatus
final case class ServerAhead(deviceEmpty: Boolean, remoteTimestamp: DateTime) extends SyncStatus
final case class DeviceAhead(serverEmpty: Boolean) extends SyncStatus

private object SyncStatus extends Logging {

  def request(syncer: Syncer)(implicit bindings: BindingModule): SyncStatus = {
    val deviceId = bindings.inject[DeviceIdService](None).getDeviceId
    val localDeviceLastSyncTimestamp = bindings.inject[InternalSyncPreferences](None).lastPushSyncTimestamp.option.map(_.withMillisOfSecond(0))
    val serverDeviceLastSyncTimestamp = syncer.getDeviceSyncTimestamp(deviceId).map(_.withMillisOfSecond(0))
    val userLastSyncTimestamp = syncer.getLastSyncTimestamp.map(_.withMillisOfSecond(0))
    log.info(s"SyncStatus: localDeviceTs=$localDeviceLastSyncTimestamp, serverDeviceTs=$serverDeviceLastSyncTimestamp, userTs=$userLastSyncTimestamp")
    (localDeviceLastSyncTimestamp, serverDeviceLastSyncTimestamp, userLastSyncTimestamp) match {
      case (Some(localDeviceTs), Some(serverDeviceTs), _           ) if localDeviceTs == serverDeviceTs => InSync
      case (Some(localDeviceTs), None                , Some(userTs)) if localDeviceTs == userTs => InSync
      case (Some(localDeviceTs), _                   , Some(userTs)) if userTs > localDeviceTs => ServerAhead(false, userTs)
      case (Some(localDeviceTs), _                   , _           ) => DeviceAhead(userLastSyncTimestamp.isEmpty)
      case (None               , _                   , Some(userTs)) => ServerAhead(true, userTs)
      case (None               , _                   , _           ) => InSync
    }
  }
}
