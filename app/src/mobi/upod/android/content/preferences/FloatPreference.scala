package mobi.upod.android.content.preferences

import android.content.SharedPreferences
import android.content.SharedPreferences.Editor

class FloatPreference(key: String, default: Float)(implicit prefs: SharedPreferences) extends Preference[Float](key) {

  def get: Float = prefs.getFloat(key, default)

  protected def write(editor: Editor, value: Float) {
    editor.putFloat(key, value)
  }

  protected def anyToValue(value: Any): Float = value.asInstanceOf[Float]
}
