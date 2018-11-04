package mobi.upod.app.services.auth

import java.io.IOException

import android.app.Activity
import android.content.{Context, Intent}
import com.escalatesoft.subcut.inject.{BindingModule, Injectable}
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.{GoogleSignInAccount, GoogleSignInOptions, GoogleSignInResult}
import com.google.android.gms.common.api.GoogleApiClient
import mobi.upod.android.logging.Logging
import mobi.upod.android.os.AsyncTask
import mobi.upod.app.services.sync.gdrive.GDriveClient

import scala.util.{Failure, Success, Try}

trait SignInListener {
  def onExplicitSignInRequired(client: SignInClient): Unit = ()
  def onSignInSucceeded(client: SignInClient, result: GoogleSignInAccount): Unit = ()
  def onSignInFailed(client: SignInClient, result: GoogleSignInResult): Unit = ()
}

class SignInClient private (context: Context, signInListener: Option[SignInListener] = None)(implicit val bindingModule: BindingModule)
  extends Injectable
  with Logging {

  private val apiClient = {
    val signInOptions = new GoogleSignInOptions.Builder().
      requestEmail.
      requestIdToken(SignInClient.Scope).
      requestScopes(GDriveClient.Scope).
      build
    new GoogleApiClient.Builder(context).
      addApi(Auth.GOOGLE_SIGN_IN_API, signInOptions).
      build
  }

  def silentSignIn(): Try[GoogleSignInResult] = {
    log.info("trying silent sign in")
    try {
      val r = apiClient.blockingConnect()
      if (r.isSuccess)
        Success(Auth.GoogleSignInApi.silentSignIn(apiClient).await)
      else
        Failure(new IOException(s"Failed to connect Google API (code: ${r.getErrorCode}): ${r.getErrorMessage}"))
    } finally apiClient.disconnect()
  }

  def asyncSilentSignIn(): Unit = {

    def handleRes(result: GoogleSignInResult): Unit = handleResult(result,
      account => signInListener.foreach(_.onSignInSucceeded(this, account)),
      _ => signInListener.foreach(_.onExplicitSignInRequired(this))
    )

    def signIn(): Unit = {
      val result = Auth.GoogleSignInApi.silentSignIn(apiClient).await()
      handleRes(result)
    }

    AsyncTask.execute(silentSignIn()) {
      case Success(result) => handleRes(result)
      case Failure(ex) =>
        log.error(ex.getMessage)
        signInListener.foreach(_.onExplicitSignInRequired(this))
    }
  }

  def getSignInIntent: Intent =
    Auth.GoogleSignInApi.getSignInIntent(apiClient)

  def handleResult(result: GoogleSignInResult, onSuccess: GoogleSignInAccount => Unit, onFailure: GoogleSignInResult => Unit): Unit = {
    if (result.isSuccess) {
      log.info(s"sign in succeeded for user ${result.getSignInAccount.getEmail}")
      onSuccess(result.getSignInAccount)
    } else {
      log.info(s"sign in failed (code: ${result.getStatus.getStatusCode}): ${result.getStatus.getStatusMessage}")
      onFailure(result)
    }
  }

  def handleResult(result: GoogleSignInResult): Unit = handleResult(result,
    account => signInListener.foreach(_.onSignInSucceeded(this, account)),
    failure => signInListener.foreach(_.onSignInFailed(this, failure))
  )

  def handleResult(data: Intent): Unit =
    handleResult(Auth.GoogleSignInApi.getSignInResultFromIntent(data))
}

object SignInClient {
  val Scope = "785398844568-hkds5lbjophe90c33houbmcv0ajd3op4.apps.googleusercontent.com"

  def apply(activity: Activity, signInListener: SignInListener)(implicit bindings: BindingModule): SignInClient =
    new SignInClient(activity, Some(signInListener))
}