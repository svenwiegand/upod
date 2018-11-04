package mobi.upod.android.preference

import android.preference.Preference.OnPreferenceChangeListener
import android.preference.Preference

object PreferenceChangeListener {
  type Listener = (Preference, AnyRef) => Boolean

  def apply(handle: Listener) = new OnPreferenceChangeListener {
    def onPreferenceChange(preference: Preference, newValue: AnyRef) =
      handle(preference, newValue)
  }
}
