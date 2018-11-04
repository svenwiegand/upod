package mobi.upod.android.content.preferences

import android.content.SharedPreferences
import android.content.SharedPreferences.Editor

class BooleanPreference(key: String, default: Boolean = false)(implicit prefs: SharedPreferences)
  extends Preference[Boolean](key) {

  def get: Boolean = prefs.getBoolean(key, default)

  protected def write(editor: Editor, value: Boolean) {
    editor.putBoolean(key, value)
  }

  protected def anyToValue(value: Any) = value.asInstanceOf[Boolean]
}
