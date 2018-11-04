package mobi.upod.app.storage

import android.app.Application
import mobi.upod.android.content.preferences._
import mobi.upod.app.R

class StoragePreferences(app: Application) extends DefaultPreferences(app, R.xml.pref_storage) {

  lazy val storageProviderType: EnumerationPreference[StorageProvider.type] with Setter[StorageProvider.StorageProviderType] = {
    val preference = new OptionalEnumerationPreference(StorageProvider)("pref_storage") with Setter[StorageProvider.StorageProviderType]
    if (preference.option.isEmpty) {
      preference := StorageProvider.getBestSuitedStorageType(app)
    }
    new EnumerationPreference(StorageProvider)("pref_storage", preference.get) with Setter[StorageProvider.StorageProviderType]
  }

  lazy val minimalFreeMegaBytes = new IntPreference("pref_storage_space_minimum", 800)

  def preferences = Seq(
    storageProviderType,
    minimalFreeMegaBytes
  )

  def storageProvider: StorageProvider = StorageProvider(app, storageProviderType)
}
