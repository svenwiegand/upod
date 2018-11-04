package mobi.upod.app.gui.preference

import android.app.Activity
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import mobi.upod.android.view.wizard._
import mobi.upod.app.gui.MainActivity
import mobi.upod.app.services.auth.SignInClient
import mobi.upod.app.services.sync.SyncService
import mobi.upod.app.storage.InternalAppPreferences
import mobi.upod.app.{AppInjection, R}

class UpdateWizardActivity extends WizardActivity with AppInjection {

  import mobi.upod.app.gui.preference.UpdateWizardActivity._

  override protected def hasNextPage(currentPageIndex: Int, currentPageKey: String): Boolean = currentPageKey match {
    case _ => false
  }

  override protected def createFirstPage: WizardPage =
    new GDrivePage

  override protected def createNextPage(currentPageIndex: Int, currentPageKey: String): WizardPage =
    throw new UnsupportedOperationException("Only a one pager")

  protected def onFinish(): Unit = {
    currentPage match {
      case page: UpdateWizardActivity.GDrivePage => page.onFinish()
      case _ => // ignore
    }
    val preferences = inject[InternalAppPreferences]
    preferences.showUpdateWizard := false
  }

  override protected val followUpActivity =
    classOf[MainActivity]
}


object UpdateWizardActivity extends AppInjection {
  private val PageKeyGDrive = "gDrive"

  def shouldBeShown: Boolean =
    inject[SyncService].isCloudSyncEnabled && inject[InternalAppPreferences].lastSignIn.option.isEmpty

  def start(activity: Activity): Unit =
    WizardActivity.start(activity, classOf[UpdateWizardActivity])

  def startInsteadOf(activity: Activity): Unit = {
    activity.finish()
    start(activity)
  }

  class GDrivePage extends WizardSignInPage(PageKeyGDrive, R.string.wizard_update_gdrive, R.string.wizard_update_gdrive_introduction, false) {

    override def onSignInSucceeded(client: SignInClient, result: GoogleSignInAccount): Unit = {
      super.onSignInSucceeded(client, result)
      onFinish()
    }
  }
}
