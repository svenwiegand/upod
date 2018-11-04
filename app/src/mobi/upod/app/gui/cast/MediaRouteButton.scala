package mobi.upod.app.gui.cast

import android.app.Activity
import android.content.{ContextWrapper, Context}
import com.escalatesoft.subcut.inject.{Injectable, BindingModule}
import mobi.upod.app.services.licensing.{PremiumMessage, LicenseService}

import scala.annotation.tailrec

class MediaRouteButton(context: Context)(implicit val bindingModule: BindingModule)
  extends android.support.v7.app.MediaRouteButton(context)
  with Injectable {

  private lazy val licenseService = inject[LicenseService]

  override def showDialog(): Boolean = {
    if (licenseService.isLicensed) {
      super.showDialog()
    } else {
      parentActivity match {
        case activity: Activity =>
          PremiumMessage.showDialog(activity)
          true
        case _ =>
          true
      }
    }
  }

  private def parentActivity: Activity = {

    @tailrec
    def findActivityContext(ctx: Context): Activity = ctx match {
      case activity: Activity => activity
      case wrapper: ContextWrapper => findActivityContext(wrapper.getBaseContext)
      case _ => null
    }

    findActivityContext(getContext)
  }
}
