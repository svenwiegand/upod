package mobi.upod.app.gui.info

import android.app.{Activity, Dialog, DialogFragment}
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.view.View
import android.webkit.WebView
import mobi.upod.app.R

class ShowLicensesDialog extends DialogFragment {

  override def onCreateDialog(savedInstanceState: Bundle): Dialog = {
    val context = getActivity

    val builder = new AlertDialog.Builder(context)
    builder.setView(createContentView)
    builder.setPositiveButton(R.string.close, null)
    builder.create
  }

  private def createContentView: View = {
    // GoogleApiAvailability.getInstance().getOpenSourceSoftwareLicenseInfo(getActivity) + licenses
    val webView = new WebView(getActivity)
    webView.loadUrl("file:///android_asset/licenses.html")
    webView
  }
}

object ShowLicensesDialog {
  private val tag = "LicensesDialog"

  def show(activity: Activity): Unit = {
    val fragment = new ShowLicensesDialog
    fragment.show(activity.getFragmentManager, tag)
  }
}