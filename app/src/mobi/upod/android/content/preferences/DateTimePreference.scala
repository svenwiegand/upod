package mobi.upod.android.content.preferences

import org.joda.time.DateTime
import android.content.SharedPreferences.Editor
import android.content.SharedPreferences

class DateTimePreference(key: String, default: Option[DateTime] = None)(implicit prefs: SharedPreferences)
  extends Preference[DateTime](key) with Optional[DateTime] {

  def option = prefs.getLong(key, 0) match {
    case 0 => default
    case millis => Some(new DateTime(millis))
  }

  protected def write(editor: Editor, value: DateTime) {
    editor.putLong(key, Option(value).map(_.getMillis).getOrElse(null.asInstanceOf[Long]))
  }

  protected def anyToValue(value: Any) = new DateTime(value.asInstanceOf[Long])
}
