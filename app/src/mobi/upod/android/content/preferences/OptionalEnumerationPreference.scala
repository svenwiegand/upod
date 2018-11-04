package mobi.upod.android.content.preferences

import android.content.SharedPreferences
import android.content.SharedPreferences.Editor
import scala.Enumeration

class OptionalEnumerationPreference[E <: Enumeration](enum: E)(key: String, default: Option[E#Value] = None)(implicit prefs: SharedPreferences)
  extends Preference[E#Value](key) with Optional[E#Value] {

  def option: Option[E#Value] = Option(prefs.getString(key, null)).map(valueByName).orElse(default)

  private def valueByName(name: String): E#Value = enum.withName(name)

  protected def write(editor: Editor, value: E#Value) {
    editor.putString(key, value.toString)
  }

  protected def anyToValue(value: Any) = valueByName(value.toString)
}
