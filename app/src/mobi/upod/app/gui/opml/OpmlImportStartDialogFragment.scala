package mobi.upod.app.gui.opml

import android.app.FragmentManager
import android.net.Uri
import android.support.v7.app.AlertDialog
import mobi.upod.android.app.{SimpleDialogFragment, SimpleDialogFragmentObject}
import mobi.upod.android.view.DialogClickListener
import mobi.upod.app.R

class OpmlImportStartDialogFragment extends SimpleDialogFragment[String] {

  protected def createDialog(data: String) = {
    val opml = Uri.parse(data)
    val builder = new AlertDialog.Builder(getActivity)
    builder.setTitle(R.string.opml_import)
    builder.setMessage(getString(R.string.opml_import_start, opml.getEncodedPath))
    builder.setPositiveButton(R.string.yes, DialogClickListener(close(_.importOpml(opml))))
    builder.setNegativeButton(R.string.no, DialogClickListener(close(_.onCancelled())))
    builder.setCancelable(false)
    builder.create()
  }

  private def importActivity = getActivity.asInstanceOf[OpmlImportActivity]

  private def close(action: OpmlImportActivity => Unit) {
    dismiss()
    action(importActivity)
  }
}

object OpmlImportStartDialogFragment extends
  SimpleDialogFragmentObject[String, OpmlImportStartDialogFragment](new OpmlImportStartDialogFragment) {

  def show(opml: Uri, fragmentManager: FragmentManager) {
    apply(opml.toString).show(fragmentManager)
  }
}
