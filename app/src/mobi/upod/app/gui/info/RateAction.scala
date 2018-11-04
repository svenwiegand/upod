package mobi.upod.app.gui.info

import android.content.Context
import com.escalatesoft.subcut.inject.{Injectable, BindingModule}
import mobi.upod.android.app.action.{ActionState, GooglePlayAction}
import mobi.upod.app.storage.InternalAppPreferences

class RateAction(alwaysEnabled: Boolean = false)(implicit val bindingModule: BindingModule)
  extends GooglePlayAction("mobi.upod.app")
  with Injectable {

  private val askToRatePreference = inject[InternalAppPreferences].mayShowRateRequest

  override def state(context: Context): ActionState.ActionState =
    if (alwaysEnabled || askToRatePreference) ActionState.enabled else ActionState.gone

  override def onFired(context: Context): Unit = {
    super.onFired(context)
    askToRatePreference := false
  }
}
