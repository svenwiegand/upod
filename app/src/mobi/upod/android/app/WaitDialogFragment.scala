package mobi.upod.android.app

import android.app.{Activity, FragmentManager, ProgressDialog}
import android.content.Context
import mobi.upod.android.logging.Logging
import mobi.upod.android.os.AsyncTask

class WaitDialogFragment extends SimpleDialogFragment[WaitDialogSpec] {
  setCancelable(false)

  protected def createDialog(data: WaitDialogSpec) = {
    val dlg = new ProgressDialog(getActivity)
    dlg.setProgressStyle(ProgressDialog.STYLE_SPINNER)
    data.titleId.foreach(dlg.setTitle)
    dlg.setMessage(getText(data.messageId))
    dlg
  }
}

object WaitDialogFragment extends SimpleDialogFragmentObject[WaitDialogSpec, WaitDialogFragment](new WaitDialogFragment) with Logging {

  def show(fragmentManager: FragmentManager, messageId: Int, titleId: Option[Int] = None, tag: String = defaultTag) {
    apply(WaitDialogSpec(messageId, titleId)).show(fragmentManager, tag)
  }

  def show(activity: Activity, messageId: Int) {
    show(activity.getFragmentManager, messageId)
  }

  def show(context: Context, messageId: Int): Unit = context match {
    case activity: Activity => show(activity, messageId)
    case _ => // ignore
  }

  def dismiss(fragmentManager: FragmentManager, tag: String = defaultTag): Unit = {
    try {
      Option(fragmentManager).flatMap(fm => Option(fm.findFragmentByTag(tag))) foreach {
        case fragment: WaitDialogFragment =>
          fragment.getDialog.dismiss()
      }
    } catch {
      case ex: Throwable =>
        log.warn("ignoring exception on wait dialog dismiss", ex)
    }
  }

  def dismiss(activity: Activity) {
    dismiss(activity.getFragmentManager)
  }

  def dismiss(context: Context): Unit = context match {
    case activity: Activity => dismiss(activity)
    case _ => // ignore
  }
}
