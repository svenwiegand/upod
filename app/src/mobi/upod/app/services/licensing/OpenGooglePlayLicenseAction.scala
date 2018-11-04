package mobi.upod.app.services.licensing

import com.crashlytics.android.answers.{CustomEvent, Answers}
import mobi.upod.android.app.action.{ActionState, GooglePlayAction}
import android.content.Context
import mobi.upod.app.App

class OpenGooglePlayLicenseAction extends GooglePlayAction(GooglePlayLicense.PackageName) {

  private def licenseService = App.inject[LicenseService]

  override def state(context: Context): ActionState.ActionState =
    if (licenseService.isPremium) ActionState.gone else ActionState.enabled

  override def onFired(context: Context): Unit = {
    App.inject[Answers].logCustom(new CustomEvent("Show License App"))
    super.onFired(context)
  }
}

object OpenGooglePlayLicenseAction {

  def intent = GooglePlayAction.defaultIntent(GooglePlayLicense.PackageName)
}