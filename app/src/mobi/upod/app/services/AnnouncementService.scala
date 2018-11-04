package mobi.upod.app.services

import com.escalatesoft.subcut.inject.{BindingModule, Injectable}
import mobi.upod.android.os.AsyncTask
import mobi.upod.app.data.Announcement
import mobi.upod.app.services.licensing.{LicenseListener, LicenseService}
import mobi.upod.app.storage.AnnouncementDao

final class AnnouncementService(implicit val bindingModule: BindingModule) extends Injectable with LicenseListener {
  private val dao = inject[AnnouncementDao]
  private val licenseService = inject[LicenseService]
  private var announcement: Option[Announcement] = None

  def init(): Unit = {
    updateCurrentAnnouncement()
    licenseService.addListener(this, false)
  }

  def currentAnnouncement: Option[Announcement] = announcement

  def dismissAnnouncement(id: Long): Unit = {
    if (announcement.exists(_.id == id)) {
      announcement = None
    }
    AsyncTask.execute {
      dao.inTransaction(dao.setDismissed(id))
      if (announcement.isEmpty)
        findNextAnnouncement
      else
        announcement
    } (announcement = _)
  }

  def onNewAnnouncement(): Unit = {
    announcement = findNextAnnouncement
  }

  private def findNextAnnouncement: Option[Announcement] =
    dao.findNextAnnouncement(!licenseService.isPremium)

  private def updateCurrentAnnouncement(): Unit =
    AsyncTask.execute(findNextAnnouncement)(announcement = _)

  //
  // license listener
  //

  override def onLicenseUpdated(licensed: Boolean): Unit = {
    super.onLicenseUpdated(licensed)
    updateCurrentAnnouncement()
  }
}