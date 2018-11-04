package mobi.upod.app.services.cloudmessaging

import com.google.firebase.messaging.{FirebaseMessagingService, RemoteMessage}
import mobi.upod.android.logging.Logging
import mobi.upod.app.AppInjection
import mobi.upod.app.services.sync.SyncService

class CloudMessagingEventService extends FirebaseMessagingService with AppInjection with Logging {
  private val ActionCrossDeviceSync = "CrossDeviceSync"

  private lazy val cloudMessagingService = inject[CloudMessagingService]
  private lazy val syncService = inject[SyncService]

  override def onMessageReceived(msg: RemoteMessage): Unit = {
    super.onMessageReceived(msg)

    val action = msg.getData.get("action")
    val srcDevice = msg.getData.get("srcDevice")
    log.info(s"received $action request from $srcDevice")

    action match {
      case ActionCrossDeviceSync => onCrossDeviceSyncAction(srcDevice)
      case _ => log.warn(s"unsupported action $action")
    }
  }

  private def onCrossDeviceSyncAction(srcDevice: String): Unit = {
    if (!cloudMessagingService.getRegistrationId.contains(srcDevice))
      syncService.requestCrossDeviceSync()
    else
      log.info("ignoring cross sync request as it was sent by this device")
  }
}
