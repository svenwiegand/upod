package mobi.upod.app.gui.preference

import android.content.Context
import android.preference.ListPreference
import android.util.AttributeSet
import mobi.upod.app.R
import mobi.upod.app.storage.StorageProvider
import mobi.upod.app.storage.StorageProvider._
import mobi.upod.util.StorageSize.DoubleStorageSize

class StorageListPreference(context: Context, attrs: AttributeSet) extends ListPreference(context, attrs) {

  updateList()

  def updateList(): Unit = {
    val storageProviders = getAvailableStorageTypes(context).map(StorageProvider(context, _))
    val writableStorageProviders = storageProviders.filter(_.writable)

    def setStorageEntries(): Unit = {
      def entryStringFor(provider: StorageProvider): CharSequence = {
        val resId = resourceStringIdFor(provider.id, R.string.pref_storage_internal, R.string.pref_storage_external, R.string.pref_storage_external_secondary)
        val availableSpace = provider.availableBytes.inGb
        context.getString(resId, availableSpace: java.lang.Double)
      }

      val entries = writableStorageProviders.map(entryStringFor).toArray
      setEntries(entries)
    }

    def setStorageValues(): Unit = {
      val values: Seq[CharSequence] = writableStorageProviders.map(_.id.name)
      setEntryValues(values.toArray)
    }

    setStorageEntries()
    setStorageValues()
  }

  def resourceStringIdFor(provider: StorageProviderType, internal: Int, external: Int, externalSecondary: Int): Int = provider match {
    case Internal => internal
    case External => external
    case ExternalSecondary => externalSecondary
  }

  def applySelectionToSummary(): Unit = {
    val storageType = StorageProvider.withName(getValue)
    val summaryId = resourceStringIdFor(
      storageType,
      R.string.pref_storage_internal_summary,
      R.string.pref_storage_external_summary,
      R.string.pref_storage_external_secondary_summary)
    setSummary(summaryId)
  }

  override def setValue(value: String): Unit = {
    super.setValue(value)
    applySelectionToSummary()
  }


}
