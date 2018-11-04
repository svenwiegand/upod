package mobi.upod.app.gui.auth

import android.app.Activity
import android.content.Intent

trait SignInActivity extends Activity with SignInUI {

  override def getActivity: Activity = this

  override def onActivityResult(requestCode: Int, resultCode: Int, data: Intent): Unit = {
    super.onActivityResult(requestCode, resultCode, data)
    onSignInResult(requestCode, resultCode, data)
  }
}
