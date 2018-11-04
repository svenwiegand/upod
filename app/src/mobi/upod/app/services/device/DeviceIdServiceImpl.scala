package mobi.upod.app.services.device

import android.content.Context
import android.provider.Settings.Secure

class DeviceIdServiceImpl(context: Context) extends DeviceIdService {
  private val id = Secure.getString(context.getContentResolver, Secure.ANDROID_ID)

  override def getDeviceId: String = id
}
