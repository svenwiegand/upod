package mobi.upod.android.content.preferences

import android.content.SharedPreferences
import mobi.upod.android.os.AsyncObservable
import android.preference.Preference.OnPreferenceChangeListener

abstract class Preference[A](val key: String)(implicit prefs: SharedPreferences)
  extends AsyncObservable[PreferenceChangeListener[A]]
  with OnPreferenceChangeListener {

  def get: A

  def getIf(condition: Boolean): Option[A] =
    if (condition) Some(get) else None

  protected def write(editor: SharedPreferences.Editor, value: A)

  protected def isNew(value: A): Boolean =
    value != get

  protected def write(value: A) {
    if (isNew(value)) {
      val editor = prefs.edit()
      write(editor, value)
      editor.apply()
      fire(_.onPreferenceChange(value))
    }
  }

  protected def anyToValue(value: Any): A

  def onPreferenceChange(preference: android.preference.Preference, newValue: Any): Boolean = {
    val value = anyToValue(newValue)
    if (value != get) {
      write(value)
    }
    fire(_.onPreferenceChange(anyToValue(newValue)))
    true
  }

  protected def fireActiveState(listener: PreferenceChangeListener[A]) = {}
}

object Preference {

  implicit def preferenceToValue[A](pref: Preference[A]): A = pref.get
}
