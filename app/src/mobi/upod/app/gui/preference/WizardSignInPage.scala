package mobi.upod.app.gui.preference

import android.content.Context
import android.view.{LayoutInflater, View, ViewGroup}
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.SignInButton
import mobi.upod.android.view.wizard.{WizardActivity, WizardPage}
import mobi.upod.android.widget.Toast
import mobi.upod.app.{App, AppInjection, R}
import mobi.upod.app.gui.auth.SignInFragment
import mobi.upod.app.services.auth.SignInClient
import mobi.upod.app.services.sync.SyncService
import mobi.upod.app.storage.{InternalAppPreferences, SyncPreferences}

import scala.util.Try

private[preference] class WizardSignInPage(key: String, headerId: Int, introductionId: Int, forceSignIn: Boolean)
  extends WizardPage(key, headerId)
  with SignInFragment
  with AppInjection {

  private lazy val lastSignInPreference = inject[InternalAppPreferences].lastSignIn

  override protected def createContentView(context: Context, container: ViewGroup, inflater: LayoutInflater): View = {
    val view = inflater.inflate(R.layout.wizard_sign_in, container)
    view.childTextView(R.id.introduction).setText(introductionId)
    val signInButton = view.childAs[SignInButton](R.id.signIn)
    signInButton.setStyle(SignInButton.SIZE_WIDE, SignInButton.COLOR_DARK)
    signInButton.onClick(explicitSignIn())
    view
  }

  override def onSignInSucceeded(client: SignInClient, result: GoogleSignInAccount): Unit = {
    super.onSignInSucceeded(client, result)
    Try(getActivity.asInstanceOf[WizardActivity].clickFinishButton())
  }

  override def validationError: Option[Int] =
    if (forceSignIn && lastSignInPreference.option.isEmpty) Some(R.string.wizard_account_required) else None

  def onFinish(): Unit = {
    if (lastSignInPreference.option.nonEmpty)
      onFinishedWithSignIn()
    else
      onFinishedWithoutSignIn()
  }

  protected def onFinishedWithSignIn(): Unit = {
    inject[SyncService].requestFullSync(true)
    Toast.show(inject[App], R.string.sync_started)
  }

  protected def onFinishedWithoutSignIn(): Unit = {
    inject[SyncPreferences].cloudSyncEnabled := forceSignIn
  }
}

