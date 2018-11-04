package mobi.upod.app.gui.auth

import android.app.Fragment
import android.content.Intent

trait SignInFragment extends Fragment with SignInUI {

  override def onActivityResult(requestCode: Int, resultCode: Int, data: Intent): Unit = {
    super.onActivityResult(requestCode, resultCode, data)
    onSignInResult(requestCode, resultCode, data)
  }
}
