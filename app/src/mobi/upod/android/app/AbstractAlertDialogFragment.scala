package mobi.upod.android.app

import android.content.DialogInterface
import android.support.v7.app.AlertDialog
import mobi.upod.android.app.action.Action

abstract class AbstractAlertDialogFragment[A <: java.io.Serializable](
    titleId: Int = 0,
    messageId: Int = 0,
    positiveButtonTextId: Option[Int] = None,
    neutralButtonTextId: Option[Int] = None,
    negativeButtonTextId: Option[Int] = None,
    positiveAction: Option[A => Action] = None,
    neutralAction: Option[A => Action] = None,
    negativeAction: Option[A => Action] = None,
    dismissAction: Option[() => Action] = None
  ) extends SimpleDialogFragment[A] with DialogInterface.OnClickListener {

  protected def title: CharSequence =
    getActivity.getString(titleId)

  protected def message: CharSequence =
    getActivity.getString(messageId)

  protected def positiveButtonText: Option[CharSequence] =
    positiveButtonTextId.map(getActivity.getString)

  protected def neutralButtonText: Option[CharSequence] =
    neutralButtonTextId.map(getActivity.getString)

  protected def negativeButtonText: Option[CharSequence] =
    negativeButtonTextId.map(getActivity.getString)

  protected def onPositiveButtonClicked(): Unit =
    positiveAction.foreach(_(dialogData).fire(getActivity))

  protected def onNeutralButtonClicked(): Unit =
    neutralAction.foreach(_(dialogData).fire(getActivity))

  protected def onNegativeButtonClicked(): Unit =
    negativeAction.foreach(_(dialogData).fire(getActivity))

  protected def onDialogDismissed(): Unit =
    dismissAction.foreach(_().fire(getActivity))

  protected def onDialogCancelled(): Unit = ()

  protected def createDialog(data: A) = {

    def initButton(text: Option[CharSequence], enableButton: CharSequence => Any): Unit =
      text.foreach(enableButton)

    val builder = new AlertDialog.Builder(getActivity)
    builder.setTitle(title)
    builder.setMessage(message)
    initButton(positiveButtonText, builder.setPositiveButton(_, this))
    initButton(neutralButtonText, builder.setNeutralButton(_, this))
    initButton(negativeButtonText, builder.setNegativeButton(_, this))

    builder.create
  }

  def onClick(dialog: DialogInterface, which: Int) {
    which match {
      case DialogInterface.BUTTON_POSITIVE =>
        onPositiveButtonClicked()
      case DialogInterface.BUTTON_NEUTRAL =>
        onNeutralButtonClicked()
      case DialogInterface.BUTTON_NEGATIVE =>
        onNegativeButtonClicked()
    }
  }

  override def onDismiss(dialog: DialogInterface): Unit = {
    super.onDismiss(dialog)
    onDialogDismissed()
  }

  override def onCancel(dialog: DialogInterface): Unit = {
    super.onCancel(dialog)
    onDialogCancelled()
  }
}
