package mobi.upod.app.gui

import mobi.upod.android.app.SimpleReloadableFragment
import com.escalatesoft.subcut.inject.Injectable
import mobi.upod.app.storage.UiPreferences
import android.os.Bundle
import android.app.Fragment
import mobi.upod.android.content.preferences.PreferenceChangeListener

private[gui] trait HideNewPreferenceListenerFragment extends Fragment with Injectable {
  self: SimpleReloadableFragment[_] =>

  private lazy val preference = inject[UiPreferences].hideNewInLibrary

  override def onStart(): Unit = {
    super.onStart()
    preference.addWeakListener(HideNewPreferenceChangedListener)
  }

  override def onStop(): Unit = {
    preference.removeListener(HideNewPreferenceChangedListener)
    super.onStop()
  }

  private object HideNewPreferenceChangedListener extends PreferenceChangeListener[Boolean] {
    override def onPreferenceChange(newValue: Boolean): Unit =
      requestReload()
  }
}