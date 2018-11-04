package mobi.upod.android.content.preferences

import android.content.SharedPreferences
import android.content.SharedPreferences.Editor
import mobi.upod.data.Mapping
import mobi.upod.data.json.{JsonReader, JsonWriter}

class ObjectPreference[A <: AnyRef](mapping: Mapping[A], key: String, default: Option[A] = None)(implicit prefs: SharedPreferences)
  extends Preference[A](key) with Optional[A] {

  private val jsonReader = JsonReader(mapping)
  private val jsonWriter = JsonWriter(mapping)

  def option: Option[A] = Option(prefs.getString(key, null)) map { jsonReader.readObject(_) }

  protected def write(editor: Editor, value: A) {
    editor.putString(key, jsonWriter.writeString(value))
  }

  protected def anyToValue(value: Any) = jsonReader.readObject(value.toString)
}
