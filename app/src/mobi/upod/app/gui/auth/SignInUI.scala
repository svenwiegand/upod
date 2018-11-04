package mobi.upod.app.gui.auth

import android.app.Activity
import android.content.Intent
import com.google.android.gms.auth.api.signin.{GoogleSignInAccount, GoogleSignInResult}
import mobi.upod.android.logging.Logging
import mobi.upod.android.widget.Toast
import mobi.upod.app.services.auth.{AuthService, SignInClient, SignInListener}
import mobi.upod.app.services.sync.SyncService
import mobi.upod.app.services.sync.gdrive.GDriveConnectionListener
import mobi.upod.app.{AppInjection, R}

trait SignInUI extends SignInListener with GDriveConnectionListener with AppInjection with Logging {
  private val SignInRequestCode = 0x5349 // ASCII "SI" :-)
  private lazy val syncService = inject[SyncService]
  private lazy val authService = inject[AuthService]
  private lazy val signInClient = SignInClient(getActivity, this)

  def getActivity: Activity

  protected def signInIfNecessary(): Unit = {
    if (syncService.isCloudSyncEnabled && authService.requiresSignIn) {
      signIn()
    }
  }

  protected def signIn(): Unit = {
    signInClient.asyncSilentSignIn()
  }

  protected def explicitSignIn(): Unit = {
    log.info("starting explicit signin")
    getActivity.startActivityForResult(signInClient.getSignInIntent, SignInRequestCode)
  }

  override def onExplicitSignInRequired(client: SignInClient): Unit = {
    super.onExplicitSignInRequired(client)
    explicitSignIn()
  }

  override def onSignInSucceeded(client: SignInClient, result: GoogleSignInAccount): Unit = {
    super.onSignInSucceeded(client, result)
    authService.onSignIn(result)
  }

  override def onSignInFailed(client: SignInClient, result: GoogleSignInResult): Unit = {
    super.onSignInFailed(client, result)
    Toast.show(getActivity, R.string.gdrive_connection_failed)
  }

  def onSignInResult(requestCode: Int, resultCode: Int, data: Intent): Unit = if (requestCode == SignInRequestCode) {
    signInClient.handleResult(data)
  }
}
