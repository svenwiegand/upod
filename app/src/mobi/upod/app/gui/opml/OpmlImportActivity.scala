package mobi.upod.app.gui.opml

import android.net.Uri
import android.os.Bundle
import android.support.v7.app.ActionBarActivity
import mobi.upod.android.app.SimpleAlertDialogFragment
import mobi.upod.android.app.action.{Action, BrowseAction, FinishActivityAction}
import mobi.upod.app.services.subscription.OpmlException
import mobi.upod.app.{AppInjection, R}

class OpmlImportActivity extends ActionBarActivity with AppInjection {

  override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)

    Option(getIntent.getData) match {
      case Some(uri) =>
        showImportDialog(uri)
      case None =>
        finish()
    }
  }

  private def showImportDialog(opml: Uri) {
    OpmlImportStartDialogFragment.show(opml, getFragmentManager)
  }

  private[opml] def onCancelled() {
    finish()
  }

  private[opml] def importOpml(opml: Uri) {
    OpmlImportProgressDialogFragment.importOpml(opml, getFragmentManager)
  }

  private def showFinishDialog(titleId: Int, msg: String, helpAction: Option[Action] = None): Unit = {
    SimpleAlertDialogFragment.showFromActivity(
      this,
      SimpleAlertDialogFragment.defaultTag,
      titleId,
      msg,
      positiveButtonTextId = helpAction.map(_ => R.string.help),
      positiveAction = helpAction,
      neutralButtonTextId = Some(R.string.close),
      dismissAction = Some(new FinishActivityAction))
  }

  private[opml] def onImportSucceeded(): Unit =
    finish()

  private[opml] def onImportFailed(error: OpmlException): Unit = {
    val helpAction = new BrowseAction("http://upod.uservoice.com/knowledgebase/articles/466362")
    showFinishDialog(R.string.opml_import_failed, error.errorTitle(this), Some(helpAction))
  }
}
