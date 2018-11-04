package mobi.upod.app.services

import java.util.Locale

import com.escalatesoft.subcut.inject.{BindingModule, Injectable}
import mobi.upod.app.AppMetaData
import mobi.upod.app.data._
import mobi.upod.app.services.licensing.LicenseService
import mobi.upod.net._
import mobi.upod.rest.WebService
import mobi.upod.util.Cursor
import org.joda.time.DateTime

class AnnouncementWebService(implicit val bindingModule: BindingModule)
  extends WebService with Injectable {

  protected val baseUrl = inject[AppMetaData].upodServiceUrl

  def getAnnouncements(since: Option[DateTime]): Cursor[Announcement] = {
    val url = url"api/3/announcement" withQueryParameters (
      "lang" -> Locale.getDefault.getLanguage,
      "premium" -> inject[LicenseService].isPremium,
      "since" -> since
      )
    get(url) asStreamOf Announcement
  }
}

