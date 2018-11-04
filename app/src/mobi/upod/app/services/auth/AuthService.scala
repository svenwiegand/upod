package mobi.upod.app.services.auth

import com.github.nscala_time.time.Imports._
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import mobi.upod.android.logging.Logging
import mobi.upod.app.AppInjection
import mobi.upod.app.storage.InternalAppPreferences
import mobi.upod.util.Observable
import org.joda.time.DateTime

trait AuthListener {
  def onSignIn(userEmail: String, idToken: String, changed: Boolean): Unit
}

class AuthService extends Observable[AuthListener] with AppInjection with Logging {
  private val SignInValidityPeriod = 1.hour
  private lazy val preferences = inject[InternalAppPreferences]

  def requiresSignIn: Boolean = preferences.lastSignIn.option match {
    case None => true
    case Some(lastSignIn) => lastSignIn + SignInValidityPeriod < DateTime.now
  }

  def onSignIn(account: GoogleSignInAccount): Unit = {
    preferences.lastSignIn := DateTime.now

    val changed = !preferences.accountEmail.option.contains(account.getEmail) || !preferences.idToken.option.contains(account.getIdToken)
    preferences.accountEmail := account.getEmail
    preferences.idToken := account.getIdToken
    log.info(s"sign in succeeded for ${account.getEmail} (idToken=${account.getIdToken})")
    fire(_.onSignIn(account.getEmail, account.getIdToken, changed))
  }

  def getUserEmail: Option[String] =
    preferences.accountEmail.option

  def getIdToken: Option[String] =
    preferences.idToken.option

  override protected def fireActiveState(listener: AuthListener): Unit = (getUserEmail, getIdToken) match {
    case (Some(userEmail), Some(idToken)) => listener.onSignIn(userEmail, idToken, false)
    case _ => // ignore
  }
}
