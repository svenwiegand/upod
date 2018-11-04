package mobi.upod.android.preference

import android.os.Bundle
import android.preference.Preference.OnPreferenceChangeListener
import android.preference.{CheckBoxPreference, Preference, PreferenceFragment, PreferenceGroup}
import mobi.upod.android.app.action.Action
import mobi.upod.android.content.preferences.{BooleanPreference, Preferences}
import mobi.upod.app.AppInjection

abstract class SimplePreferenceFragment(preferencesResId: Int)
  extends PreferenceFragment
  with OnPreferenceChangeListener
  with AppInjection {

  setRetainInstance(true)

  protected def prefs: Option[Preferences]

  protected def conditionalPreferences: Map[CharSequence, Boolean] = Map()

  protected def changeListeners: Map[CharSequence, OnPreferenceChangeListener] = Map()

  protected def clickActions: Map[CharSequence, Action] = Map()

  override def onCreate(savedInstanceState: Bundle) {

    def hideUnsupported(): Unit = {
      val unsupportedPreferences = conditionalPreferences filter { case (_, supported) => !supported }
      unsupportedPreferences.keys.foreach(removePreference)
    }

    def registerChangeListeners(): Unit = {
      
      def recursiveRegisterChangeListener(group: PreferenceGroup): Unit = {
        for (i <- 0 until group.getPreferenceCount) {
          group.getPreference(i) match {
            case subGroup: PreferenceGroup =>
              recursiveRegisterChangeListener(subGroup)
            case pref: Preference =>
              pref.setOnPreferenceChangeListener(this)
          }
        }        
      }
      
      recursiveRegisterChangeListener(getPreferenceScreen)
    }

    def registerClickActions(): Unit = clickActions.foreach { case (key, action) =>
      findPreference(key).setOnPreferenceClickListener(PreferenceClickListener { preference =>
        if (action.isEnabled(getActivity)) {
          action.fire(getActivity)
          true
        } else {
          false
        }
      })
    }

    super.onCreate(savedInstanceState)
    addPreferencesFromResource(preferencesResId)
    registerChangeListeners()
    registerClickActions()
    hideUnsupported()
  }

  def onPreferenceChange(preference: Preference, newValue: scala.Any): Boolean = {

    def informPreference(): Boolean = {
      prefs.foreach { preferences =>
        preferences.find(preference.getKey).foreach(_.onPreferenceChange(preference, newValue))
      }
      true
    }

    changeListeners.get(preference.getKey) match {
      case Some(listener) =>
        if (listener.onPreferenceChange(preference, newValue))
          informPreference()
        else
          false
      case None =>
        informPreference()
    }
  }

  protected def enablePreference(key: String, enable: Boolean): Unit =
    findPreference(key).setEnabled(enable)

  protected def enablePreference(pref: mobi.upod.android.content.preferences.Preference[_], enable: Boolean): Unit =
    enablePreference(pref.key, enable)

  protected def checkBoxPreference(pref: BooleanPreference): CheckBoxPreference =
    findPreference(pref.key).asInstanceOf[CheckBoxPreference]

  protected def checkPreference(pref: BooleanPreference, checked: Boolean): Unit =
    checkBoxPreference(pref).setChecked(checked)

  protected def removePreference(key: CharSequence): Unit =
    Option(findPreference(key)).foreach(getPreferenceScreen.removePreference)
}
