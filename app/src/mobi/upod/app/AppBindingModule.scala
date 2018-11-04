package mobi.upod.app

import android.app.{AlarmManager, NotificationManager}
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import com.crashlytics.android.answers.Answers
import com.escalatesoft.subcut.inject.NewBindingModule
import com.evernote.android.job.JobManager
import mobi.upod.android.app.{DelayedAlarmManager, NavigationSettings}
import mobi.upod.android.os.PowerManager
import mobi.upod.android.view.DisplayMetrics
import mobi.upod.app.gui.CoverartLoader
import mobi.upod.app.services._
import mobi.upod.app.services.auth.AuthService
import mobi.upod.app.services.cast.MediaRouteService
import mobi.upod.app.services.cloudmessaging.CloudMessagingService
import mobi.upod.app.services.device.{DeviceIdService, DeviceIdServiceImpl}
import mobi.upod.app.services.download.DownloadService
import mobi.upod.app.services.licensing.LicenseService
import mobi.upod.app.services.net.ConnectionStateRetriever
import mobi.upod.app.services.playback.PlaybackService
import mobi.upod.app.services.storage.StorageService
import mobi.upod.app.services.subscription.SubscriptionService
import mobi.upod.app.services.sync._
import mobi.upod.app.storage._

class AppBindingModule(app: App) extends NewBindingModule(
  implicit module => {
    import module._

    // basic configuration
    bind [App] toSingle app
    bind [AppMetaData] toSingle new AppMetaData(app.getPackageManager.getApplicationInfo(app.getPackageName, PackageManager.GET_META_DATA).metaData)
    bind [DisplayMetrics] toSingle new DisplayMetrics(app)

    // preferences
    bind [InternalAppPreferences] toSingle new InternalAppPreferences(app)
    bind [InternalSyncPreferences] toSingle new InternalSyncPreferences(app)
    bind [SyncPreferences] toSingle new SyncPreferences(app)
    bind [UiPreferences] toSingle new UiPreferences(app)
    bind [PlaybackPreferences] toSingle new PlaybackPreferences(app)
    bind [DownloadPreferences] toSingle new DownloadPreferences(app)
    bind [StoragePreferences] toSingle new StoragePreferences(app)
    bind [SupportPreferences] toSingle new SupportPreferences(app)

    // storage layer
    val databaseHelper = DatabaseHelper(app)
    bind [DatabaseHelper] toProvider databaseHelper
    bind [Database] toProvider databaseHelper.writable
    bind [PodcastDao] toProvider databaseHelper.podcastDao
    bind [EpisodeDao] toProvider databaseHelper.episodeDao
    bind [EpisodeListChangeDao] toProvider databaseHelper.episodeListChangeDao
    bind [SubscriptionChangeDao] toProvider databaseHelper.subscriptionChangeDao
    bind [PodcastColorChangeDao] toProvider databaseHelper.podcastColorChangeDao
    bind [ImportedSubscriptionsDao] toProvider databaseHelper.importedSubscriptionsDao
    bind [AnnouncementDao] toProvider databaseHelper.announcementDao
    bind [CoverartProvider] toSingle new CoverartProvider(app)

    // service layer
    bind [AuthService] toSingle new AuthService
    bind [CloudMessagingService] toSingle new CloudMessagingService
    bind [DeviceIdService] toSingle new DeviceIdServiceImpl(app)
    bind [ConnectionStateRetriever] toSingle new ConnectionStateRetriever(app)
    bind [StorageService] toSingle new StorageService
    bind [PodcastFetchService] toSingle new PodcastFetchService
    bind [PodcastWebService] toSingle new PodcastWebService
    bind [PodcastDirectoryWebService] toSingle new PodcastDirectoryWebService
    bind [AnnouncementWebService] toProvider new AnnouncementWebService
    bind [SubscriptionService] toSingle new SubscriptionService
    bind [EpisodeService] toSingle new EpisodeService
    bind [DownloadService] toSingle new DownloadService
    bind [PlaybackService] toSingle new PlaybackService
    bind [NavigationSettings] toSingle new NavigationSettingsService(app)
    bind [OnlinePodcastService] toProvider new OnlinePodcastService
    bind [LicenseService] toSingle new LicenseService
    bind [MediaRouteService] toSingle new MediaRouteService
    bind [SyncService] toSingle new SyncService(app)
    bind [AnnouncementService] toSingle new AnnouncementService
    bind [Answers] toProvider Answers.getInstance()

    // GUI layer
    bind [CoverartLoader] toSingle new CoverartLoader

    // android stuff
    bind [AlarmManager] toProvider app.getSystemService(Context.ALARM_SERVICE).asInstanceOf[AlarmManager]
    bind [AudioManager] toProvider app.getSystemService(Context.AUDIO_SERVICE).asInstanceOf[AudioManager]
    bind [DelayedAlarmManager] toProvider new DelayedAlarmManager(app)
    bind [PowerManager] toProvider new PowerManager(app)
    bind [NotificationManager] toProvider app.getSystemService(Context.NOTIFICATION_SERVICE).asInstanceOf[NotificationManager]

}) with AppDependencies {

  override def onAppCreate(): Unit = {

    def initService[A <: Any : scala.Predef.Manifest](): A =
      inject[A](None)

    def initServices(): Unit = {
      initService[MediaRouteService]().registerRouteChangeCallbacks()
      initService[AnnouncementService]().init()
      initService[CloudMessagingService]().init()
      initService[LicenseService]()
      initService[SyncService]()
      initService[DownloadService]()
    }

    JobManager.create(app)
    initServices()
  }

  override def onUpgrade(oldVersion: Int, newVersion: Int): Unit = {
    def upgradable[A <: AppUpgradeListener](implicit manifest: scala.Predef.Manifest[A]): A =
      inject[A](None)

    Seq[AppUpgradeListener](
      upgradable[InternalSyncPreferences],
      upgradable[InternalAppPreferences],
      upgradable[SyncPreferences],
      upgradable[DownloadPreferences],
      new ImageFetcher()(this)
    ).foreach(_.onAppUpgrade(oldVersion, newVersion))
  }

  override def onAppTerminate(): Unit = {
    inject[MediaRouteService](None).unregisterRouteChangeCallbacks()
    inject[DatabaseHelper](None).close()
  }
}
