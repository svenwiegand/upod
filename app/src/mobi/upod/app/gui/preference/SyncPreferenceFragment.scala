package mobi.upod.app.gui.preference

import android.content.pm.PackageManager
import android.preference.Preference.OnPreferenceChangeListener
import android.preference.{CheckBoxPreference, Preference}
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import mobi.upod.android.app.action.Action
import mobi.upod.android.app.permission.PermissionRequestingActivity
import mobi.upod.android.content.preferences.{OptionSetter, TimePreference}
import mobi.upod.android.preference.{PreferenceChangeListener, SimplePreferenceFragment, TimePickerPreference}
import mobi.upod.android.util.ApiLevel
import mobi.upod.app.R
import mobi.upod.app.gui.auth.SignInFragment
import mobi.upod.app.services.auth.SignInClient
import mobi.upod.app.storage.{InternalSyncPreferences, SyncPreferences}
import mobi.upod.util.DateTimeUtils.RichDateTime
import mobi.upod.util.TimeOfDay
import org.joda.time.DateTime


class SyncPreferenceFragment
  extends SimplePreferenceFragment(R.xml.pref_sync)
  with PremiumPreferences
  with SignInFragment {

  import mobi.upod.app.gui.preference.SyncPreferenceFragment._

  private val syncPreferences = inject[SyncPreferences]
  private val internalSyncPreferences = inject[InternalSyncPreferences]
  private lazy val cloudSyncPreference = findPreference(PrefCloudSync).asInstanceOf[CheckBoxPreference]
  private lazy val timeInfoPreference = findPreference(PrefSyncTimeInfo)

  protected def prefs = Some(inject[SyncPreferences])

  override protected val premiumPreferences: Seq[CharSequence] = Seq(
    PrefCloudSync,
    PrefFixGDriveAppFolder,
    "pref_sync_time_1_enabled",
    "pref_sync_time_2_enabled"
  )

  override protected val conditionalPreferences: Map[CharSequence, Boolean] = Map(
    PrefFixGDriveAppFolder -> (ApiLevel >= ApiLevel.Marshmallow)
  )

  override protected val clickActions: Map[CharSequence, Action] = Map(
    PrefFixGDriveAppFolder -> Action(fixGDriveAppFolder())
  )

  override def onStart(): Unit = {
    super.onStart()
    updateSyncTimeInfo()
    internalSyncPreferences.lastFullSyncTimestamp.addWeakListener(SyncTimeInfoPreferenceListener)
    internalSyncPreferences.nextSyncTimestamp.addWeakListener(SyncTimeInfoPreferenceListener)
  }

  override def onStop(): Unit = {
    internalSyncPreferences.lastFullSyncTimestamp.removeListener(SyncTimeInfoPreferenceListener)
    internalSyncPreferences.nextSyncTimestamp.removeListener(SyncTimeInfoPreferenceListener)
    super.onStop()
  }

  override protected def changeListeners: Map[CharSequence, OnPreferenceChangeListener] = Map(
    PrefCloudSync -> PreferenceChangeListener(onCloudSyncChanged),
    PrefSyncTime1Enabled -> PreferenceChangeListener(onSyncTimeEnabledChanged(_, _, PrefSyncTime1, syncPreferences.syncTime1)),
    PrefSyncTime2Enabled -> PreferenceChangeListener(onSyncTimeEnabledChanged(_, _, PrefSyncTime2, syncPreferences.syncTime2)),
    PrefSyncTime1 -> PreferenceChangeListener(onSyncTimeChanged(_, _, syncPreferences.syncTime1)),
    PrefSyncTime2 -> PreferenceChangeListener(onSyncTimeChanged(_, _, syncPreferences.syncTime2))
  )


  private def onSyncTimeEnabledChanged(pref: Preference, newValue: AnyRef, timePrefKey: String, targetPref: TimePreference with OptionSetter[TimeOfDay]): Boolean = {
    val enabled = newValue.asInstanceOf[Boolean]
    if (enabled) {
      val time = TimeOfDay(findPreference(timePrefKey).asInstanceOf[TimePickerPreference].getTime)
      targetPref := time
    } else {
      targetPref := None
    }
    true
  }

  private def onSyncTimeChanged(pref: Preference, newValue: AnyRef, targetPref: TimePreference with OptionSetter[TimeOfDay]): Boolean = {
    val time = TimeOfDay(newValue.toString)
    targetPref := time
    true
  }

  private def updateSyncTimeInfo(): Unit = {

    def textFor(timestamp: Option[DateTime]): String = timestamp match {
      case Some(t) => t.formatRelativeDate(getActivity)
      case None => getActivity.getString(R.string.not_applicable)
    }

    val lastSyncTime = textFor(internalSyncPreferences.lastFullSyncTimestamp.option)
    val nextSyncTime = textFor(internalSyncPreferences.nextSyncTimestamp.option)
    val summary = getActivity.getString(R.string.pref_sync_time_info_summary, lastSyncTime, nextSyncTime)
    timeInfoPreference.setSummary(summary)
  }

  private def onCloudSyncChanged(pref: Preference, newValue: AnyRef): Boolean = {
    val enabled = newValue.asInstanceOf[java.lang.Boolean].booleanValue
    if (enabled) {
      signIn()
      false
    } else {
      true
    }
  }

  override def onSignInSucceeded(client: SignInClient, result: GoogleSignInAccount): Unit = {
    super.onSignInSucceeded(client, result)
    cloudSyncPreference.setChecked(true)
  }

  private object SyncTimeInfoPreferenceListener extends mobi.upod.android.content.preferences.PreferenceChangeListener[DateTime] {

    override def onPreferenceChange(newValue: DateTime): Unit =
      updateSyncTimeInfo()
  }

  private def fixGDriveAppFolder(): Unit = {
    getActivity.asInstanceOf[PermissionRequestingActivity].ensureHasPermission(
      android.Manifest.permission.GET_ACCOUNTS,
      result => if (result == PackageManager.PERMISSION_GRANTED) new FixGDriveAppFolderAction().fire(getActivity)
    )
  }
}

private object SyncPreferenceFragment {
  val PrefCloudSync = "pref_sync_cloud"
  val PrefFixGDriveAppFolder = "pref_fix_gdrive_app_folder"
  val PrefSyncTimeInfo = "pref_sync_time_info"
  val PrefSyncTime1Enabled = "pref_sync_time_1_enabled"
  val PrefSyncTime1 = "pref_sync_time_1"
  val PrefSyncTime2Enabled = "pref_sync_time_2_enabled"
  val PrefSyncTime2 = "pref_sync_time_2"
}