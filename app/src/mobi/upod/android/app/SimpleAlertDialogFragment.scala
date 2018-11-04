package mobi.upod.android.app

import android.app.{Activity, Fragment, FragmentManager}
import android.content.DialogInterface
import mobi.upod.android.app.FragmentTransactions.RichFragmentManager
import mobi.upod.android.app.action.Action
import mobi.upod.android.logging.Logging

class SimpleAlertDialogFragment extends AbstractAlertDialogFragment[AlertDialogSpec]() with DialogInterface.OnClickListener {

  override protected def title: CharSequence =
    if (dialogData.titleId != 0) getActivity.getString(dialogData.titleId) else null

  override protected def message: CharSequence =
    dialogData.message

  override protected def positiveButtonText: Option[CharSequence] =
    dialogData.positiveButtonTextId.map(getActivity.getString)

  override protected def neutralButtonText: Option[CharSequence] =
    dialogData.neutralButtonTextId.map(getActivity.getString)

  override protected def negativeButtonText: Option[CharSequence] =
    dialogData.negativeButtonTextId.map(getActivity.getString)

  private def listener: Option[AlertDialogListener] = {
    val callingFragment = dialogData.listenerFragmentId flatMap { id =>
      Option(getFragmentManager).flatMap(fragmentManager => Option(fragmentManager.findFragmentById(id)))
    }
    val listenerFragment = callingFragment.collect { case l: AlertDialogListener => l }
    listenerFragment.orElse(getActivity match {
      case listener: AlertDialogListener => Some(listener)
      case _ => None
    })
  }

  private def fireAction(action: Option[Action]) {
    action.foreach { a =>
      if (a.isEnabled(getActivity)) {
        a.fire(getActivity)
      }
    }
  }

  private def fireListener(event: AlertDialogListener => String => Unit) {
    listener.foreach(event(_)(dialogData.dialogTag))
  }

  override protected def onPositiveButtonClicked(): Unit = {
    fireAction(dialogData.positiveAction)
    fireListener(_.onPositiveAlertButtonClicked)
  }

  override protected def onNeutralButtonClicked(): Unit = {
    fireAction(dialogData.neutralAction)
    fireListener(_.onNeutralAlertButtonClicked)
  }

  override protected def onNegativeButtonClicked(): Unit = {
    fireAction(dialogData.negativeAction)
    fireListener(_.onNegativeAlertButtonClicked)
  }

  override protected def onDialogDismissed(): Unit = {
    fireAction(dialogData.dismissAction)
    listener foreach { _.onAlertDialogDismissed(dialogData.dialogTag) }
  }

  override protected def onDialogCancelled(): Unit =
    listener foreach { _.onAlertDialogCancelled(dialogData.dialogTag) }
}

object SimpleAlertDialogFragment
  extends SimpleDialogFragmentObject[AlertDialogSpec, SimpleAlertDialogFragment](new SimpleAlertDialogFragment)
  with Logging {

  def show(
    caller: Fragment,
    dialogTag: String,
    titleId: Int,
    message: CharSequence,
    positiveButtonTextId: Option[Int] = None,
    neutralButtonTextId: Option[Int] = None,
    negativeButtonTextId: Option[Int] = None,
    positiveAction: Option[Action] = None,
    neutralAction: Option[Action] = None,
    negativeAction: Option[Action] = None,
    dismissAction: Option[Action] = None) {

    val listener = caller match {
      case listener: AlertDialogListener => Some(listener.getId)
      case _ => None
    }

    val spec = AlertDialogSpec(
      listener,
      dialogTag,
      titleId,
      message,
      positiveButtonTextId,
      neutralButtonTextId,
      negativeButtonTextId,
      positiveAction,
      neutralAction,
      negativeAction,
      dismissAction
    )

    try apply(spec).show(caller.getFragmentManager, dialogTag) catch {
      case e: IllegalStateException => log.error("Failed to show alert dialog", e)
    }
  }

  def showFromActivity(
    activity: Activity,
    dialogTag: String,
    titleId: Int,
    message: CharSequence,
    positiveButtonTextId: Option[Int] = None,
    neutralButtonTextId: Option[Int] = None,
    negativeButtonTextId: Option[Int] = None,
    positiveAction: Option[Action] = None,
    neutralAction: Option[Action] = None,
    negativeAction: Option[Action] = None,
    dismissAction: Option[Action] = None): Unit = {

    val spec = AlertDialogSpec(
      None,
      dialogTag,
      titleId,
      message,
      positiveButtonTextId,
      neutralButtonTextId,
      negativeButtonTextId,
      positiveAction,
      neutralAction,
      negativeAction,
      dismissAction
    )

    try apply(spec).show(activity.getFragmentManager, dialogTag) catch {
      case e: IllegalStateException => log.error("Faile to show allert dialog", e)
    }
  }

  def ensureDismissed(fragmentManager: FragmentManager, dialogTag: String): Unit = {
    Option(fragmentManager.findFragmentByTag(dialogTag)) foreach { dlgFragment =>
      fragmentManager.inTransaction(_.remove(dlgFragment))
    }
  }

  def ensureDismissed(activity: Activity, dialogTag: String = defaultTag): Unit =
    ensureDismissed(activity.getFragmentManager, dialogTag)
}
