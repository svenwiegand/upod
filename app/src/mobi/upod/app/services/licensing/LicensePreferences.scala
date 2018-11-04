package mobi.upod.app.services.licensing

import android.app.Application
import android.content.Context
import java.util.NoSuchElementException
import com.google.android.vending.licensing.{AESObfuscator, PreferenceObfuscator}
import mobi.upod.app.services.licensing.LicenseStatus.LicenseStatus
import org.joda.time.DateTime
import mobi.upod.android.logging.Logging

private[licensing] final class LicensePreferences(app: Application) extends Logging {

  import LicensePreferences._

  private val prefs = {
    val sp = app.getSharedPreferences(File, Context.MODE_PRIVATE)
    new PreferenceObfuscator(sp, new AESObfuscator(app))
  }

  def licenseStatus_=(status: LicenseStatus): Unit =
    edit(prefs.putString(LastStatus, status.toString))

  def licenseStatus: LicenseStatus = try {
    LicenseStatus.withName(prefs.getString(LastStatus, LicenseStatus.Unlicensed.toString))
  } catch {
    case ex: NoSuchElementException =>
      LicenseStatus.Unlicensed
  }

  def startTrial(): DateTime = {
    val trialStart = DateTime.now
    prefs.putDateTime(TrialStart, trialStart)
    prefs.commit()
    trialStart
  }

  def trialStart: Option[DateTime] =
    Option(prefs.getDateTime(TrialStart, null))

  private def edit(block: => Unit): Unit = {
    block
    prefs.commit()
  }
}

private object LicensePreferences {
  val File = "_licensing"
  val LastStatus = "lastStatus"
  val TrialStart = "signature_" // obfuscate meaning of this field
}