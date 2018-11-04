package mobi.upod.app.gui.preference

import android.os.Bundle
import android.preference.Preference
import mobi.upod.android.app.{AlertDialogListener, SimpleAlertDialogFragment, WaitDialogFragment}
import mobi.upod.android.os.AsyncTask
import mobi.upod.android.preference.{PreferenceChangeListener, SimplePreferenceFragment}
import mobi.upod.app.R
import mobi.upod.app.services.EpisodeService
import mobi.upod.app.services.sync.SyncService
import mobi.upod.app.storage.UiPreferences


class UiPreferenceFragment extends SimplePreferenceFragment(R.xml.pref_ui) with AlertDialogListener {
  private val DialogTagConfirmSkipInbox = "confirmSkipInbox"
  private lazy val episodeService = inject[EpisodeService]
  private lazy val syncService = inject[SyncService]
  private lazy val uiPreferences = inject[UiPreferences]
  private lazy val skipNewPreference = uiPreferences.skipNew
  private lazy val autoAddToPlaylistPreference = uiPreferences.autoAddToPlaylist
  private lazy val hideNewInLibraryPreference = uiPreferences.hideNewInLibrary
  private lazy val notifyNewEpisodesPreference = uiPreferences.notifyNewEpisodes

  protected def prefs = Some(uiPreferences)

  override protected def changeListeners = Map(
    skipNewPreference.key -> PreferenceChangeListener(onSkipNewUpdated),
    autoAddToPlaylistPreference.key -> PreferenceChangeListener(onAutoAddToPlaylistChanged)
  )

  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)
    enableNewEpisodePreferences(!skipNewPreference)
  }

  private def onAutoAddToPlaylistChanged(pref: Preference, newValue: AnyRef): Boolean = {
    syncService.identitySettingsUpdated()
    true
  }

  private def onSkipNewUpdated(pref: Preference, newValue: AnyRef): Boolean = {
    val activate = newValue.asInstanceOf[java.lang.Boolean].booleanValue
    if (activate) {
      SimpleAlertDialogFragment.show(
        this,
        DialogTagConfirmSkipInbox,
        R.string.pref_skip_new_confirmation_title,
        getActivity.getString(R.string.pref_skip_new_confirmation),
        Some(R.string.yes),
        negativeButtonTextId = Some(R.string.no))
      false
    } else {
      episodeService.skipNew(false)
      enableNewEpisodePreferences(true)
      true
    }
  }

  override def onPositiveAlertButtonClicked(dialogTag: String): Unit = dialogTag match {
    case DialogTagConfirmSkipInbox => skipNew()
  }

  def skipNew(): Unit = {
    WaitDialogFragment.show(getActivity, R.string.wait_please)
    AsyncTask.execute[Unit](episodeService.skipNew(true)) { _ =>
      WaitDialogFragment.dismiss(getActivity)
      checkPreference(skipNewPreference, true)
      checkPreference(hideNewInLibraryPreference, false)
      checkPreference(notifyNewEpisodesPreference, false)
      enableNewEpisodePreferences(false)
    }
  }

  private def enableNewEpisodePreferences(enable: Boolean): Unit = {
    enablePreference(hideNewInLibraryPreference, enable)
    enablePreference(notifyNewEpisodesPreference, enable)
  }
}