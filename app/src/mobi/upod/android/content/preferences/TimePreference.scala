package mobi.upod.android.content.preferences

import android.content.SharedPreferences
import android.content.SharedPreferences.Editor
import mobi.upod.util.TimeOfDay

class TimePreference(key: String, default: Option[TimeOfDay] = None)(implicit prefs: SharedPreferences)
  extends Preference[TimeOfDay](key)
  with Optional[TimeOfDay] {

  override def option: Option[TimeOfDay] =
    Option(prefs.getString(key, default.map(_.toString).orNull)).map(TimeOfDay.apply)

  override protected def write(editor: Editor, value: TimeOfDay): Unit =
    editor.putString(key, Option(value).map(_.toString).orNull)

  override protected def anyToValue(value: Any): TimeOfDay =
    TimeOfDay(value.toString)
}
