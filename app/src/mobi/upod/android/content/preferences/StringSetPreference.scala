package mobi.upod.android.content.preferences

import android.content.SharedPreferences
import scala.collection.JavaConverters._
import android.content.SharedPreferences.Editor

class StringSetPreference(key: String, default: Set[String] = Set())(implicit prefs: SharedPreferences)
  extends Preference[Set[String]](key) {

  override def get: Set[String] =
    Option(prefs.getStringSet(key, null)).map(_.asScala.toSet).getOrElse(default)

  override protected def write(editor: Editor, value: Set[String]): Unit =
    editor.putStringSet(key, value.asJava)

  override protected def anyToValue(value: Any): Set[String] =
    value.asInstanceOf[java.util.Set[String]].asScala.toSet
}
