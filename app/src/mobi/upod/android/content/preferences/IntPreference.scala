package mobi.upod.android.content.preferences

import android.content.SharedPreferences
import android.content.SharedPreferences.Editor

class IntPreference(key: String, default: Int)(implicit prefs: SharedPreferences) extends Preference[Int](key) {

  def get: Int = prefs.getInt(key, default)

  protected def write(editor: Editor, value: Int) {
    editor.putInt(key, value)
  }

  protected def anyToValue(value: Any): Int = value.asInstanceOf[Int]
}
