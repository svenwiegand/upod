package mobi.upod.android.content.preferences

import android.content.SharedPreferences
import android.content.SharedPreferences.Editor

class StringPreference(key: String, default: Option[String] = None)(implicit prefs: SharedPreferences)
  extends Preference[String](key)
  with Optional[String] {

  def this(key: String, default: String)(implicit prefs: SharedPreferences) = this(key, Option(default))

  def option: Option[String] = Option(prefs.getString(key, default.orNull))

  protected def write(editor: Editor, value: String) {
    editor.putString(key, value)
  }

  protected def anyToValue(value: Any) = value.toString
}
