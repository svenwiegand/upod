package mobi.upod.app.gui.preference

import android.app.Activity
import mobi.upod.android.view.wizard.{ValueChoice, _}
import mobi.upod.app.gui.MainActivity
import mobi.upod.app.services.licensing.LicenseService
import mobi.upod.app.storage._
import mobi.upod.app.{App, AppInjection, R}

final class StartupWizardActivity extends WizardActivity with StoragePermissionRequestActivity with AppInjection {

  import mobi.upod.app.gui.preference.StartupWizardActivity._

  override protected def hasNextPage(currentPageIndex: Int, currentPageKey: String): Boolean = currentPageKey match {
    case PageKeyWelcome => true
    case PageKeyCloudSync => true
    case _ => false
  }

  override protected def createFirstPage: WizardPage =
    new WelcomePage

  override protected def createNextPage(currentPageIndex: Int, currentPageKey: String): WizardPage = currentPageKey match {
    case PageKeyWelcome => userType match {
      case Some(NewUser) => new NewUserPage
      case Some(ExistingUser) => new CloudSyncPage
    }
    case PageKeyCloudSync => inject[SyncPreferences].cloudSyncEnabled.get match {
      case true => new SignInPage
      case false => new RecurringUserPage
    }
  }

  override protected def onFinishButtonClicked(): Unit = {
    ensureExternalStoragePermission(true)
  }

  protected def onFinish(): Unit = {
    val preferences = inject[InternalAppPreferences]
    preferences.showStartupWizard := false
  }

  override protected val followUpActivity =
    classOf[MainActivity]

  override protected val requestPermissionOnStart: Boolean = false

  override protected def shouldRequestExternalStoragePermission: Boolean = true

  override protected[preference] def onStorageAvailable(): Unit = {
    super.onStorageAvailable()
    currentPage match {
      case p: SignInPage => p.onFinish()
      case _ => // nothing to do
    }
    finishWizard()
  }
}

object StartupWizardActivity {
  private val PageKeyWelcome = "welcome"
  private val PageKeyNewUser = "newUser"
  private val PageKeyCloudSync = "cloudSync"
  private val PageKeyRecurringUser = "recurringUser"
  private val PageKeyAccount = "account"

  private val NewUser = 0
  private val ExistingUser = 1

  private var userType: Option[Int] = None

  def start(activity: Activity): Unit = {
    WizardActivity.start(activity, classOf[StartupWizardActivity])
  }

  def startInsteadOf(activity: Activity): Unit = {
    activity.finish()
    start(activity)
  }

  class WelcomePage extends SimpleSingleChoicePage[Int](
    PageKeyWelcome,
    R.string.wizard_welcome,
    R.string.wizard_welcome_introduction,
    0,
    userType,
    choice => userType = Some(choice),
    ValueChoice(NewUser, R.string.wizard_welcome_option_new_user),
    ValueChoice(ExistingUser, R.string.wizard_welcome_option_existing_user)
  )

  class NewUserPage extends WizardWebPage(PageKeyNewUser, R.string.wizard_new_user, R.string.wizard_new_user_introduction)

  class CloudSyncPage extends SimpleSingleChoicePage[Boolean](
    PageKeyCloudSync,
    R.string.wizard_cloud_sync,
    R.string.wizard_cloud_sync_introduction,
    0,
    None,
    sync => App.inject[SyncPreferences].cloudSyncEnabled := sync,
    ValueChoice(false, R.string.wizard_cloud_sync_no),
    ValueChoice(true, R.string.wizard_cloud_sync_yes)
  ) {
    private lazy val licenseService = App.inject[LicenseService]

    override def onResume(): Unit = {
      super.onResume()
      licenseService.checkLicense()
    }

    override def shouldShow: Boolean = userType.isEmpty || userType.contains(ExistingUser)

    override def validationError: Option[Int] = super.validationError.orElse(
      if (!App.inject[SyncPreferences].cloudSyncEnabled || licenseService.isPremium) None else Option(R.string.wizard_cloud_sync_license_required)
    )
  }

  class RecurringUserPage extends WizardWebPage(PageKeyRecurringUser, R.string.wizard_recurring_user, R.string.wizard_recurring_user_introduction)

  class SignInPage extends WizardSignInPage(PageKeyAccount, R.string.wizard_account, R.string.wizard_account_required, true)
}
