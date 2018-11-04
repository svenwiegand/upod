package mobi.upod.app.storage

import android.app.Application
import com.github.nscala_time.time.Imports._
import mobi.upod.android.content.preferences._
import mobi.upod.app.AppUpgradeListener
import mobi.upod.app.gui.info.SponsorRequestCardHeaders
import org.joda.time.DateTime

class InternalAppPreferences(app: Application) extends DevicePreferences(app) with AppUpgradeListener {

  lazy val installationDate: DateTime = {
    val pref = new DateTimePreference("installation_date") with Setter[DateTime]
    pref.option match {
      case Some(date) =>
        date
      case _ =>
        val date = DateTime.now
        pref := date
        date
    }
  }

  lazy val appVersion = new IntPreference("app_version", 0) with Setter[Int]
  lazy val lastSignIn = new DateTimePreference("gdrive_last_connection") with Setter[DateTime]
  lazy val accountEmail = new StringPreference("accout_email") with Setter[String]
  lazy val idToken = new StringPreference("id_token") with Setter[String]
  lazy val fcmRegistrationId = new StringPreference("fcm_registration_id") with Setter[String]
  lazy val fcmDeviceGroupNotificationKey = new StringPreference("fcm_device_group_notification_key") with Setter[String]

  lazy val openDrawerOnStart = new BooleanPreference("open_drawer_on_start", true) with Setter[Boolean]
  lazy val showStartupWizard = new BooleanPreference("show_startup_wizard", true) with Setter[Boolean]
  lazy val showUpdateWizard = new BooleanPreference("show_update_wizard", false) with Setter[Boolean]
  lazy val showTrialExpiredMessage = new BooleanPreference("show_trial_expired_message", true) with Setter[Boolean]

  lazy val mayShowRateRequest = new BooleanPreference("may_show_rate_request", true) with Setter[Boolean]
  lazy val lastRateRequest = new DateTimePreference("last_rate_request", Some(installationDate + SponsorRequestCardHeaders.RequestGap)) with Setter[DateTime]

  lazy val mayShowPurchaseRequest = new BooleanPreference("may_show_purchase_request", true) with Setter[Boolean]
  lazy val lastPurchaseRequest = new DateTimePreference("last_purchase_request", Some(installationDate)) with Setter[DateTime]

  lazy val mediaRouteId = new StringPreference("active_media_route") with OptionSetter[String]
  lazy val castSessionId = new StringPreference("active_cast_session") with OptionSetter[String]

  lazy val showTooMuchNewEpisodesWarning = new BooleanPreference("show_too_much_new_warning", false) with Setter[Boolean]

  def preferences = Seq(
    appVersion,
    openDrawerOnStart
  )

  override def onAppUpgrade(oldVersion: Int, newVersion: Int) = {
    if (oldVersion > 0) {
      showStartupWizard := false
    }
    if (oldVersion < 404) {
      showTooMuchNewEpisodesWarning := true
    }
    if (oldVersion < 6004) {
      showUpdateWizard := true
    }
  }
}
