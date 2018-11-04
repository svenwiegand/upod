package mobi.upod.app.services.storage

import com.escalatesoft.subcut.inject.{Injectable, BindingModule}
import mobi.upod.android.logging.Logging
import mobi.upod.app.storage.{StorageProvider, StoragePreferences}
import mobi.upod.app.storage.StorageState._
import mobi.upod.util.Observable
import mobi.upod.android.content.preferences.PreferenceChangeListener
import mobi.upod.app.storage.StorageProvider.StorageProviderType

class StorageService(implicit val bindingModule: BindingModule)
  extends Observable[StorageStateListener]
  with Injectable
  with Logging {

  private lazy val storagePreferences = inject[StoragePreferences]
  private var _storageState: StorageState = NotAvailable

  updateState()
  storagePreferences.storageProviderType.addWeakListener(StorageTypeChangeListener)

  def storageProvider = storagePreferences.storageProvider

  def storageState = storageProvider.state

  private[storage] def updateState(): Unit = {
    val newState = storageState
    if (newState != _storageState) {
      val oldState = _storageState
      _storageState = newState
      log.crashLogInfo(s"storage state changed from $oldState to $newState")
      fire(_.onStorageStateChanged(oldState, newState))
    }
  }

  def swtichTo(storageProviderType: StorageProviderType): Unit = {
    if (storageProviderType != storagePreferences.storageProviderType.get) {
      storagePreferences.storageProviderType := storageProviderType
      fire(_.onStorageProviderTypeChanged(storageProviderType))
    }
  }

  def onExternalStoragePermissionGranted(): Unit =
    fire(_.onExternalStoragePermissionGranted())

  protected def fireActiveState(listener: StorageStateListener): Unit = {
    listener.onStorageProviderTypeChanged(storagePreferences.storageProviderType)
    val state = storageState
    if (state != NotAvailable) {
      listener.onStorageStateChanged(NotAvailable, state)
    }
  }

  private object StorageTypeChangeListener extends PreferenceChangeListener[StorageProvider.StorageProviderType] {

    override def onPreferenceChange(newValue: StorageProviderType): Unit =
      updateState()
  }
}
