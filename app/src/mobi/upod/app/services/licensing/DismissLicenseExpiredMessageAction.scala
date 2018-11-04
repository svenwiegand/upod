package mobi.upod.app.services.licensing

import android.content.Context
import mobi.upod.android.app.action.Action
import mobi.upod.app.App
import mobi.upod.app.storage.InternalAppPreferences

class DismissLicenseExpiredMessageAction extends Action {

  override def onFired(context: Context): Unit = {
    App.inject[InternalAppPreferences].showTrialExpiredMessage := false
  }
}
