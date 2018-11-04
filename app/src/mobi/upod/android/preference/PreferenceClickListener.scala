package mobi.upod.android.preference

import android.preference.Preference.OnPreferenceClickListener
import android.preference.Preference

object PreferenceClickListener {
  type Listener = Preference => Boolean

  def apply(handle: Listener) = new OnPreferenceClickListener {
    def onPreferenceClick(preference: Preference): Boolean =
      handle(preference)
  }
}
