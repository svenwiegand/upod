package mobi.upod.app.services.cloudmessaging

import com.google.firebase.iid.FirebaseInstanceId
import mobi.upod.android.logging.Logging
import mobi.upod.android.os.AsyncTask.AsyncTaskExecutionContext
import mobi.upod.app.AppInjection
import mobi.upod.app.services.auth.{AuthListener, AuthService}
import mobi.upod.app.storage.InternalAppPreferences

import scala.concurrent.Future

class CloudMessagingService extends AuthListener with AppInjection with Logging {
  private val authService = inject[AuthService]
  private lazy val preferences = inject[InternalAppPreferences]
  private lazy val webService = new CloudMessagingWebService

  def init(): Unit = {
    authService.addListener(this)
  }

  def getRegistrationId: Option[String] =
    Option(FirebaseInstanceId.getInstance.getToken)

  def registerDeviceIfPossible(): Unit = (authService.getUserEmail, getRegistrationId, authService.getIdToken) match {
    case (Some(userEmail), Some(registrationId), Some(idToken)) =>
      Future {
        try {
          val notificationKey = new CloudMessagingDeviceGroupWebService().addNotificationKey(userEmail, registrationId, idToken)
          preferences.fcmDeviceGroupNotificationKey := notificationKey
          log.info(s"successfully registered device for user $userEmail. Got notification key $notificationKey")
        } catch {
          case error: Throwable =>
            log.error("Failed to register device", error)
        }
      }
    case _ =>
      log.info("skipped registering device as at least one required information isn't available")
  }

  def sendCrossDeviceSyncRequest(): Unit =
    preferences.fcmDeviceGroupNotificationKey.option.foreach(webService.postCrossSyncMessage)

  override def onSignIn(userEmail: String, idToken: String, changed: Boolean): Unit =
    if (changed || preferences.fcmDeviceGroupNotificationKey.option.isEmpty) registerDeviceIfPossible()

  private[cloudmessaging] def onRegistrationIdRefresh(): Unit = {
    val newId = FirebaseInstanceId.getInstance.getToken
    if (!preferences.fcmRegistrationId.option.contains(newId) || preferences.fcmDeviceGroupNotificationKey.option.isEmpty) {
      preferences.fcmRegistrationId := newId
      registerDeviceIfPossible()
    }
  }
}
