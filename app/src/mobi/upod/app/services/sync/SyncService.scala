package mobi.upod.app.services.sync

import java.net.URI

import android.content.Context
import com.escalatesoft.subcut.inject.{BindingModule, Injectable}
import com.evernote.android.job.JobRequest.NetworkType
import com.evernote.android.job.{Job, JobCreator, JobManager, JobRequest}
import com.github.nscala_time.time.Imports._
import mobi.upod.android.content.preferences.{FunctionalPreferenceChangeListener, TimePreference}
import mobi.upod.android.job.{ConnectedJobRequestBuilder, SimpleJob}
import mobi.upod.android.logging.Logging
import mobi.upod.android.os.AsyncObservable
import mobi.upod.app.services.licensing.LicenseService
import mobi.upod.app.storage.{InternalSyncPreferences, PlaybackPreferences, SyncPreferences, UiPreferences}
import org.joda.time.DateTime

final class SyncService(context: Context)(implicit val bindingModule: BindingModule)
  extends AsyncObservable[SyncListener]
  with JobCreator
  with Injectable
  with Logging {

  private val PushSyncDelay = 45.seconds.millis
  private val PushSyncDelayTolerance = 5.seconds.millis
  private val FullSyncDelayTolerance = 3.minutes.millis

  private lazy val internalSyncPreferences = inject[InternalSyncPreferences]
  private lazy val syncPreferences = inject[SyncPreferences]
  private lazy val licenseService = inject[LicenseService]

  private var runningSyncType: Option[String] = None

  def running = runningSyncType.nonEmpty

  init()

  private def init(): Unit = {

    def initAutomaticSync(): Unit = {
      JobManager.instance.addJobCreator(this)
      ensureAutomaticSyncIsScheduled()
    }

    registerIdentitySettingsListeners()
    registerSyncTimeChangeListener()
    initAutomaticSync()
  }

  private def registerIdentitySettingsListeners(): Unit = {

    def markSyncRequired[A]: FunctionalPreferenceChangeListener[A] =
      FunctionalPreferenceChangeListener[A](identitySettingsUpdated())

    def registerUiPreferencesListeners(): Unit = {
      val p = inject[UiPreferences]
      p.hideNewInLibrary.addSynchronousListener(markSyncRequired)
      p.autoAddToPlaylist.addSynchronousListener(markSyncRequired)
      p.skipNew.addSynchronousListener(markSyncRequired)
      p.showMediaTypeFilter.addSynchronousListener(markSyncRequired)
    }

    def registerPlaybackPreferencesListener(): Unit = {
      val p = inject[PlaybackPreferences]
      p.mediaTimeFormat.addSynchronousListener(markSyncRequired)
      p.notificationButtons.addSynchronousListener(markSyncRequired)
      p.fastForwardSeconds.addSynchronousListener(markSyncRequired)
      p.rewindSeconds.addSynchronousListener(markSyncRequired)
    }

    registerUiPreferencesListeners()
    registerPlaybackPreferencesListener()
  }

  private def registerSyncTimeChangeListener(): Unit = {
    syncPreferences.syncFrequencyInMinutes.addListener(FunctionalPreferenceChangeListener(_ => scheduleAutomaticSync()))
    syncPreferences.syncTime1.addListener(FunctionalPreferenceChangeListener(_ => scheduleAutomaticSync()))
    syncPreferences.syncTime2.addListener(FunctionalPreferenceChangeListener(_ => scheduleAutomaticSync()))
    syncPreferences.syncOnlyOnWifi.addListener(FunctionalPreferenceChangeListener(_ => scheduleAutomaticSync()))
  }

  def isCloudSyncEnabled: Boolean =
    licenseService.isLicensed && syncPreferences.cloudSyncEnabled

  def ensureAutomaticSyncIsScheduled(): Unit = {
    if (JobManager.instance.getAllJobRequestsForTag(SyncJob.TagFullSync).isEmpty) {
      scheduleAutomaticSync(10.seconds.millis)
    }
  }

  def scheduleAutomaticSync(minDelay: Long = 0): Unit = {
    val now = DateTime.now

    def optionalTimeFor(pref: TimePreference): Option[DateTime] =
      pref.getIf(licenseService.isLicensed).map(_.next)

    def scheduleFor(delay: Long): Unit = {
      if (delay <= 1000) {
        requestFullSync()
      } else {
        SyncJob.schedule(SyncJob.TagFullSync, syncPreferences.syncOnlyOnWifi, delay, FullSyncDelayTolerance)
        log.info(s"next automatic sync in ${delay / 1000 / 60} minutes (+ <=${FullSyncDelayTolerance / 1000 / 60})")
      }
    }

    val nextAutomaticSyncTime: Option[DateTime] = {
      val times = Seq(
        optionalTimeFor(syncPreferences.syncTime1),
        optionalTimeFor(syncPreferences.syncTime2),
        internalSyncPreferences.lastFullSyncTimestamp.option match {
          case Some(lastFullSync) => syncPreferences.syncFrequency.map(lastFullSync + _)
          case None => syncPreferences.syncFrequency.map(now + _)
        }
      )
      val availableTimes = times.collect { case Some(t) => t }
      (if (availableTimes.nonEmpty) Some(availableTimes.min) else None) match {
        case Some(time) if time < now => Some(now)
        case Some(time) => Some(time)
        case _ => None
      }
    }

    nextAutomaticSyncTime match {
      case Some(nast) =>
        log.info(s"next automatic sync requested for $nast")
      case None =>
        log.info("no automatic sync configured")
    }
    internalSyncPreferences.nextSyncTimestamp := nextAutomaticSyncTime

    nextAutomaticSyncTime.map(nast => math.max(minDelay, (now to nast).millis)) match {
      case Some(delay) => scheduleFor(delay)
      case None => JobManager.instance.cancelAllForTag(SyncJob.TagFullSync)
    }
  }

  private def schedulePushSync(): Unit = if (isCloudSyncEnabled) {
    SyncJob.schedule(SyncJob.TagPushSync, syncPreferences.syncOnlyOnWifi, PushSyncDelay, PushSyncDelayTolerance)
  }

  def pushSyncRequired(): Unit = {
    internalSyncPreferences.pushSyncRequired := true
    schedulePushSync()
  }

  def playlistUpdated() {
    internalSyncPreferences.playlistUpdated := true
    pushSyncRequired()
  }

  def identitySettingsUpdated() {
    internalSyncPreferences.identitySettingsUpdated := true
    pushSyncRequired()
  }

  def requestFullSync(forceNow: Boolean = false, syncConflictResolution: Option[SyncConflictResolution.Value] = None): Unit = {
    internalSyncPreferences.fullSyncRequired := true
    if (forceNow) {
      log.info("starting immediate sync")
      SyncServiceImpl.requestFullSync(context, syncConflictResolution)
    } else {
      log.info("scheduling sync as soon as network connection is available")
      SyncJob.scheduleImmediate(context, SyncJob.TagFullSync, syncPreferences.syncOnlyOnWifi)
    }
  }

  def requestCrossDeviceSync(): Unit = {
    runningSyncType match {
      case Some(SyncServiceImpl.CrossDeviceSyncAction | SyncServiceImpl.FullSyncAction) =>
        log.info("skipping cross device sync, as a sync is currently running")
      case _ =>
        SyncJob.scheduleImmediate(context, SyncJob.TagCrossDeviceSync, syncPreferences.syncOnlyOnWifi)
    }
  }

  def fullSyncRequired() {
    pushSyncRequired()
    requestFullSync()
  }

  def syncPodcast(podcast: URI): Unit =
    SyncServiceImpl.requestPodcastSync(context, podcast)

  protected def fireActiveState(listener: SyncListener) {
    if (running) {
      listener.onSyncStarted()
    }
  }

  private[sync] def onSyncStarted(syncAction: String) {
    runningSyncType = Some(syncAction)
    fire { _.onSyncStarted() }
  }

  private[sync] def onSyncFinished() {
    runningSyncType = None
    fire { _.onSyncFinished() }
  }

  override def create(tag: String): Job =
    SyncJob.create(tag, context)
}

private object SyncJob {
  val TagFullSync = "sync_full"
  val TagPushSync = "sync_push"
  val TagCrossDeviceSync = "sync_cross_device"

  def create(tag: String, context: Context): Job = tag match {
    case TagFullSync => SimpleJob(SyncServiceImpl.requestFullSync(context))
    case TagPushSync => SimpleJob(SyncServiceImpl.requestPushSync(context))
    case TagCrossDeviceSync => SimpleJob(SyncServiceImpl.requestCrossDeviceSync(context))
    case _ => null
  }

  def scheduleImmediate(context: Context, tag: String, requiresWifi: Boolean): Unit =
    ConnectedJobRequestBuilder.scheduleImmediate(context, tag, requiresWifi, true)

  def schedule(tag: String, requiresWifi: Boolean, delay: Long, tolerance: Long): Unit = new JobRequest.Builder(tag).
    setUpdateCurrent(true).
    setRequiredNetworkType(if (requiresWifi) NetworkType.UNMETERED else NetworkType.CONNECTED).
    setRequirementsEnforced(true).
    setExecutionWindow(math.max(1000, delay), delay + tolerance).
    build.
    schedule()
}