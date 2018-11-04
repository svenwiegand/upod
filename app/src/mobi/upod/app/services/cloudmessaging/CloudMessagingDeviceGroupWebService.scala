package mobi.upod.app.services.cloudmessaging

import mobi.upod.app.services.cloudmessaging.CloudMessagingDeviceGroupWebService.{NotificationOperation, NotificationOperationResult}
import mobi.upod.data.{Mapping, MappingProvider}
import mobi.upod.rest.{WebService, WrappedHttpResponse}

private class CloudMessagingDeviceGroupWebService extends WebService {
  private val SenderID = "785398844568"
  override protected val baseUrl: String = "https://android.googleapis.com/gcm"

  def addNotificationKey(userEmail: String, registrationId: String, idToken: String): String =
    postOperation("add", userEmail, registrationId, idToken).as(NotificationOperationResult).notificationKey

  def removeNotificationKey(userEmail: String, registrationId: String, idToken: String): Unit =
    postOperation("remove", userEmail, registrationId, idToken)


  private def postOperation(operation: String, userEmail: String, registrationId: String, idToken: String): WrappedHttpResponse = {
    post(
      "googlenotification",
      NotificationOperation(operation, userEmail, registrationId, idToken),
      NotificationOperation,
      "project_id" -> SenderID
    )
  }
}

private object CloudMessagingDeviceGroupWebService {

  case class NotificationOperation(
    operation: String,
    notificationKeyName: String,
    registrationIds: Seq[String],
    idToken: String
  )

  object NotificationOperation extends MappingProvider[NotificationOperation] {
    import mobi.upod.data.Mapping._

    def apply(operation: String, notificationKeyName: String, registrationId: String, idToken: String): NotificationOperation =
      apply(operation, notificationKeyName, Seq(registrationId), idToken)

    override val mapping: Mapping[NotificationOperation] = map(
      "operation" -> string,
      "notification_key_name" -> string,
      "registration_ids" -> seq(string),
      "id_token" -> string
    )(apply)(unapply)
  }

  case class NotificationOperationResult(notificationKey: String)

  object NotificationOperationResult extends MappingProvider[NotificationOperationResult] {
    import mobi.upod.data.Mapping._

    override val mapping: Mapping[NotificationOperationResult] = map(
      "notification_key" -> string
    )(apply)(unapply)
  }
}