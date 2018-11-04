package mobi.upod.android.preference

import android.preference.ListPreference
import android.content.Context
import android.util.AttributeSet
import mobi.upod.app.R
import mobi.upod.util.Collections._

class ListPreferenceWithEntrySummary(context: Context, attrs: AttributeSet) extends ListPreference(context, attrs) {

  val summaries: IndexedSeq[CharSequence] = {
    val xmlValues = context.obtainStyledAttributes(attrs, R.styleable.ListPreferenceWithEntrySummary)
    val results = Option(xmlValues.getTextArray(R.styleable.ListPreferenceWithEntrySummary_summaries)) match {
      case Some(strings) => strings.toIndexedSeq
      case _ => throw new IllegalArgumentException("missing attribute 'summaries'")
    }
    xmlValues.recycle()
    results
  }

  require(getEntries.size == summaries.size, s"summaries array should have ${getEntries.size} elements but has ${summaries.size} elements instead")
  applySelectionToSummary()

  def currentSummary: Option[CharSequence] = {
    val entryIndex = getEntryValues.indexOf(getValue).validIndex
    entryIndex.map(summaries(_))
  }

  def applySelectionToSummary() {
    currentSummary.foreach(setSummary)
  }

  override def setValue(value: String) {
    super.setValue(value)
    applySelectionToSummary()
  }
}
