package mobi.upod.app.services.storage

import android.content.{Intent, Context, BroadcastReceiver}
import mobi.upod.app.AppInjection

class StorageStateReceiver extends BroadcastReceiver with AppInjection {

  def onReceive(context: Context, intent: Intent): Unit =
    inject[StorageService].updateState()
}
