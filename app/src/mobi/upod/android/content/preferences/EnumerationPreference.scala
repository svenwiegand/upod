package mobi.upod.android.content.preferences

import android.content.SharedPreferences
import scala.Enumeration
import android.content.SharedPreferences.Editor

class EnumerationPreference[E <: Enumeration](enum: E)(key: String, default: E#Value)(implicit prefs: SharedPreferences)
  extends Preference[E#Value](key) {

  def get: E#Value = Option(prefs.getString(key, null)).map(valueByName).getOrElse(default)

  private def valueByName(name: String): E#Value = enum.withName(name)

  protected def write(editor: Editor, value: E#Value) {
    editor.putString(key, value.toString)
  }

  protected def anyToValue(value: Any) = valueByName(value.toString)
}
