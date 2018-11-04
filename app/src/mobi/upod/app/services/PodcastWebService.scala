package mobi.upod.app.services

import com.escalatesoft.subcut.inject.{BindingModule, Injectable}
import com.google.api.client.http._
import mobi.upod.app.data._
import mobi.upod.app.services.sync._
import mobi.upod.app.{App, AppMetaData}
import mobi.upod.data.Mapping
import mobi.upod.rest.WebService

class PodcastWebService(implicit val bindingModule: BindingModule)
  extends WebService with Injectable {

  protected val baseUrl = inject[AppMetaData].upodServiceUrl
  private val appVersion = inject[App].appVersion.toString

  def updatePodcastColors(podcastColorChanges: Seq[PodcastColorChange]): Unit =
    post ("api/3/podcastColors", podcastColorChanges, Mapping.seq(PodcastColorChange))

  override protected def prepareRequest(request: HttpRequest) {
    super.prepareRequest(request)
    request.getHeaders.set("X-ClientVersion", appVersion)
  }

  override protected def mapHttpException(ex: HttpResponseException): Throwable = {
    ex.getStatusCode match {
      case 400 if ex.errorCode == Some("expiredClientVersion") =>
        new AppVersionExpiredException(inject[App])
      case _ =>
        super.mapHttpException(ex)
    }
  }
}

