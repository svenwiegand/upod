package mobi.upod.app.services.cloudmessaging

import com.google.firebase.iid.FirebaseInstanceIdService
import mobi.upod.android.logging.Logging
import mobi.upod.app.AppInjection

class CloudMessagingInstanceIDService extends FirebaseInstanceIdService with AppInjection with Logging {

  override def onTokenRefresh(): Unit = {
    log.info("received cloud messaging instance ID")
    super.onTokenRefresh()
    inject[CloudMessagingService].onRegistrationIdRefresh()
  }
}
