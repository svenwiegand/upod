package mobi.upod.app.services.sync

object SyncConflictResolution extends Enumeration {
  type SyncConflictResolution = Val
  val UseServerState = Value
  val UseDeviceState = Value
}
