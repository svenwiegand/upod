package mobi.upod.app.services.storage

import mobi.upod.app.storage.StorageProvider.StorageProviderType
import mobi.upod.app.storage.StorageState

trait StorageStateListener {

  def onExternalStoragePermissionGranted(): Unit = ()

  def onStorageProviderTypeChanged(newStorageProviderType: StorageProviderType): Unit = ()

  def onStorageStateChanged(oldState: StorageState.StorageState, newState: StorageState.StorageState): Unit = ()
}
