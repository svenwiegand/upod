package mobi.upod.app.gui.opml

import android.app.FragmentManager
import android.net.Uri
import com.escalatesoft.subcut.inject.BindingModule
import mobi.upod.android.app._
import mobi.upod.app.R
import mobi.upod.app.services.subscription.{OpmlException, OpmlImportCallback, SubscriptionService}

class OpmlImportProgressDialogFragment private
  extends WaitDialogFragment
  with OpmlImportCallback
  with AlertDialogListener
  with FragmentStateHolder {

  override def onImportSucceeded(): Unit = if (state.started) {
    dismiss()
    importActivity.onImportSucceeded()
  }

  override def onImportFailed(error: OpmlException): Unit = if (state.started) {
    dismiss()
    importActivity.onImportFailed(error)
  }

  private def importActivity = getActivity.asInstanceOf[OpmlImportActivity]
}

object OpmlImportProgressDialogFragment extends
  SimpleDialogFragmentObject[WaitDialogSpec, OpmlImportProgressDialogFragment](new OpmlImportProgressDialogFragment) {

  def importOpml(opml: Uri, fragmentManager: FragmentManager)(implicit bindingModule: BindingModule) {
    val fragment = apply(WaitDialogSpec(R.string.opml_import_progress))
    fragment.show(fragmentManager)

    val subscriptionService = bindingModule.inject[SubscriptionService](None)
    subscriptionService.asyncImportOpml(opml, fragment)
  }
}