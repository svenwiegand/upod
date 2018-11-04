package mobi.upod.app.services.cloudmessaging

import com.escalatesoft.subcut.inject.{BindingModule, Injectable}
import com.google.api.client.http.{HttpRequest, HttpResponseException}
import mobi.upod.app.App
import mobi.upod.app.AppMetaData
import mobi.upod.app.services.auth.AuthService
import mobi.upod.app.services.sync.AppVersionExpiredException
import mobi.upod.net._
import mobi.upod.rest.WebService

class CloudMessagingWebService(implicit val bindingModule: BindingModule) extends WebService with Injectable {
  private lazy val authService = inject[AuthService]
  private lazy val cloudMessagingService = inject[CloudMessagingService]
  private val appVersion = inject[App].appVersion
  override protected val baseUrl = inject[AppMetaData].upodServiceUrl

  def postCrossSyncMessage(notificationKey: String): Unit = {
    val url = url"api/4/notification/sync" withQueryParameters (
      "notificationKey" -> notificationKey
    )
    post(url)
  }

  override protected def prepareRequest(request: HttpRequest): Unit = {
    super.prepareRequest(request)

    request.getHeaders.
      set("X-ClientVersion", appVersion).
      set("X-AuthProvider", "google").
      set("X-AuthToken", authService.getIdToken)
    cloudMessagingService.getRegistrationId.foreach { deviceId =>
      request.getHeaders.set("X-DeviceId", deviceId)
    }
  }

  override protected def mapHttpException(ex: HttpResponseException): Throwable = ex.getStatusCode match {
    case 400 if ex.errorCode.contains("expiredClientVersion") =>
      new AppVersionExpiredException(inject[App])
    case _ =>
      super.mapHttpException(ex)
  }
}
