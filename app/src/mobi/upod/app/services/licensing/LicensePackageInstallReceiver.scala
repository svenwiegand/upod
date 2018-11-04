package mobi.upod.app.services.licensing

import android.content.{Intent, Context, BroadcastReceiver}
import mobi.upod.app.AppInjection
import mobi.upod.android.logging.Logging

class LicensePackageInstallReceiver
  extends BroadcastReceiver
  with AppInjection
  with Logging {

  def onReceive(context: Context, intent: Intent): Unit = {
    log.info(s"received license module action ${intent.getAction} (${intent.getData})")
    inject[LicenseService].checkLicense()
  }
}
