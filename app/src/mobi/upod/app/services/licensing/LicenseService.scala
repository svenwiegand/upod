package mobi.upod.app.services.licensing

import com.escalatesoft.subcut.inject.{BindingModule, Injectable}
import com.evernote.android.job.{Job, JobCreator, JobManager}
import com.github.nscala_time.time.Imports._
import mobi.upod.android.job.{ConnectedJobRequestBuilder, SimpleJob}
import mobi.upod.android.logging.Logging
import mobi.upod.android.os.AsyncObservable
import mobi.upod.app.App

import scala.util.Try

class LicenseService(implicit val bindingModule: BindingModule)
  extends AsyncObservable[LicenseListener]
  with JobCreator
  with Injectable
  with Logging {

  private val app = inject[App]
  private val prefs = new LicensePreferences(app)
  private val googlePlayLicenseChecker = new GooglePlayLicenseChecker(app, GooglePlayLicenseCallback)
  private var _hasCurrentStatus = false
  private var _licenseStatus = prefs.licenseStatus

  init()

  private def init(): Unit = {
    checkLicense()
    JobManager.instance.addJobCreator(this)
  }

  def hasCurrentStatus = _hasCurrentStatus
  
  def licenseStatus = _licenseStatus

  def isPremium = _licenseStatus != LicenseStatus.Unlicensed

  private def trialEnd: Option[DateTime] =
    prefs.trialStart.map(_ + LicenseService.TrialPeriod)

  def remainingTrialPeriod: Option[Duration] = trialEnd flatMap { end =>
    val now = DateTime.now
    if (end > now)
      Some((now to end).duration)
    else
      None
  }

  def isTrial: Boolean = trialEnd match {
    case Some(end) =>
      val trialExpired = DateTime.now > end
      !isPremium && !trialExpired
    case None =>
      false
  }
  
  def isTrialExpired: Boolean = 
    trialEnd.exists(_ < DateTime.now)
  
  def canStartTrial: Boolean = 
    !isTrial && !isTrialExpired
  
  def startTrial(): Unit =
    prefs.startTrial()

  def isLicensed =
    isPremium || isTrial

  def checkLicense(): Unit = {
    log.trace("starting license check")
    googlePlayLicenseChecker.checkLicense()
  }

  def destroy(): Unit = {
    googlePlayLicenseChecker.destroy()
  }

  private def updateStatus(status: LicenseStatus.LicenseStatus, checkSucceeded: Boolean): Unit = {
    log.info(s"updating license status to $status")
    _licenseStatus = status
    Try(prefs.licenseStatus = status).recover{case error => log.crashLogError("failed to update license status", error)} // sometimes this fails with a BufferUnderflowException, so lets ignore it, as this is only the cached status
    _hasCurrentStatus = checkSucceeded
    if (isPremium)
      fire(_.onLicensed())
    else
      fire(_.onNotLicensed())
    
    if (checkSucceeded) cancelLicenseCheckSchedule()
  }

  private def onLicenseCheckFailed(): Unit = {
    log.info(s"license check failed - keeping previous state")
    updateStatus(_licenseStatus, false)

    if (!_hasCurrentStatus) scheduleLicenseCheck()
  }

  private def scheduleLicenseCheck(): Unit =
    ConnectedJobRequestBuilder.schedule(LicenseService.JobTagLicenseCheck, false)

  private def cancelLicenseCheckSchedule(): Unit =
    JobManager.instance.cancelAllForTag(LicenseService.JobTagLicenseCheck)

  override def create(tag: String): Job = tag match {
    case LicenseService.JobTagLicenseCheck => SimpleJob(checkLicense())
    case _ => null
  }

  protected def fireActiveState(listener: LicenseListener): Unit = {
    if (isLicensed)
      fire(_.onLicensed())
    else
      fire(_.onNotLicensed())
  }

  private object GooglePlayLicenseCallback extends LicenseCheckerCallback {
    def onLicensed(): Unit =
      updateStatus(LicenseStatus.GooglePlayLicense, true)

    def onNotLicensed(): Unit =
      updateStatus(LicenseStatus.Unlicensed, true)

    def onLicenseCheckFailed(errorCode: Int): Unit =
      LicenseService.this.onLicenseCheckFailed()
  }
}

object LicenseService {
  val TrialPeriod = 7.days
  val JobTagLicenseCheck = "license_check"
}