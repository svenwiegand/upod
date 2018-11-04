package mobi.upod.android.logging

import android.content.Context
import android.net.Uri
import mobi.upod.android.app.action.{ActionWaitDialog, AsyncAction}
import mobi.upod.app.{AppInjection, R}

class SendDiagnosticsAction(subject: String = "")
  extends AsyncAction[Unit, Iterable[Uri]]
  with ActionWaitDialog
  with AppInjection {

  override protected val waitDialogMessageId = R.string.collecting_diagnostics

  protected def getData(context: Context) = ()

  protected def processData(context: Context, data: Unit): Iterable[Uri] = {
    Diagnostics.writeInfoLog(context)
    Diagnostics.worldReadableLogs(context) ++ 
      Diagnostics.worldReadablePrefs(context) ++ 
      Diagnostics.worldReadableDatabases(context, Iterable("upod"))
  }

  override protected def postProcessData(context: Context, result: Iterable[Uri]): Unit = {
    super.postProcessData(context, result)
    Diagnostics.sendDiagnostics(context, result, subject)
  }
}
