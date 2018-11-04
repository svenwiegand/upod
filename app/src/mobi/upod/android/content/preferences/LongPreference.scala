package mobi.upod.android.content.preferences

import android.content.SharedPreferences
import android.content.SharedPreferences.Editor

class LongPreference(key: String, default: Long)(implicit prefs: SharedPreferences) extends Preference[Long](key) {

  def get: Long = prefs.getLong(key, default)

  protected def write(editor: Editor, value: Long) {
    editor.putLong(key, value)
  }

  protected def anyToValue(value: Any) = value.asInstanceOf[Long]
}
