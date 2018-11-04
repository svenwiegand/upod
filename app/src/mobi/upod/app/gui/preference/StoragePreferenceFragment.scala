package mobi.upod.app.gui.preference

import android.os.Bundle
import android.preference.{ListPreference, Preference}
import mobi.upod.android.app.AlertDialogListener
import mobi.upod.android.app.action.Action
import mobi.upod.android.logging.Logging
import mobi.upod.android.preference.{PreferenceChangeListener, SimplePreferenceFragment}
import mobi.upod.app.R
import mobi.upod.app.services.storage.{StorageStateListener, StorageService}
import mobi.upod.app.storage.StorageProvider.StorageProviderType
import mobi.upod.app.storage.{StoragePreferences, StorageProvider}


class StoragePreferenceFragment
  extends SimplePreferenceFragment(R.xml.pref_storage)
  with AlertDialogListener
  with StorageStateListener
  with Logging {

  private val PrefStorageRequestPermission = "pref_storage_request_permission"
  private val PrefStorage = "pref_storage"

  private val storagePreferences = inject[StoragePreferences]
  private val storageService = inject[StorageService]
  protected def prefs = Some(storagePreferences)

  initDefaults()

  override protected def conditionalPreferences: Map[CharSequence, Boolean] = Map(
    PrefStorageRequestPermission -> !StorageProvider.hasExternalStoragePermissions(getActivity)
  )

  override protected def changeListeners = Map(
    PrefStorage -> PreferenceChangeListener(onStorageUpdated)
  )

  override protected def clickActions: Map[CharSequence, Action] = Map(
    PrefStorageRequestPermission -> new EnsureExternalStoragePermissionAction
  )

  private def initDefaults(): Unit =
    storagePreferences.storageProviderType.get

  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)
    storageService.addListener(this)
    enablePreference(PrefStorage, StorageProvider.hasExternalStoragePermissions(getActivity))
  }

  override def onDestroy(): Unit = {
    storageService.removeListener(this)
    super.onDestroy()
  }

  private[preference] def setStorage(storageType: StorageProviderType): Unit = {
    findPreference(PrefStorage).asInstanceOf[ListPreference].setValue(storageType.name)
    inject[StorageService].swtichTo(storageType)
  }

  private def onStorageUpdated(pref: Preference, newValue: AnyRef): Boolean = {
    val oldStorageType = storagePreferences.storageProviderType.get
    val newStorageType = StorageProvider.withName(newValue.toString)
    if (oldStorageType != newStorageType) {
      val request = new SwitchStorageRequest(oldStorageType.id, newStorageType.id, getId)
      SwitchStorageConfirmationDialogFragment.show(getActivity, request)
    }
    false
  }

  private def updateStoragePicker(): Unit = {
    val pref = findPreference(PrefStorage).asInstanceOf[StorageListPreference]
    val enable = StorageProvider.hasExternalStoragePermissions(getActivity)
    pref.setEnabled(StorageProvider.hasExternalStoragePermissions(getActivity))
    if (enable) {
      pref.updateList()
      removePreference(PrefStorageRequestPermission)
    }
  }

  //
  // StorageStateListener
  //

  override def onExternalStoragePermissionGranted(): Unit =
    updateStoragePicker()
}
