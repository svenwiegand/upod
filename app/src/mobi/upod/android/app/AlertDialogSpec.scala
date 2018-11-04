package mobi.upod.android.app

import mobi.upod.android.app.action.Action

private[app] case class AlertDialogSpec(
  listenerFragmentId: Option[Int],
  dialogTag: String,
  titleId: Int,
  message: CharSequence,
  positiveButtonTextId: Option[Int],
  neutralButtonTextId: Option[Int],
  negativeButtonTextId: Option[Int],
  positiveAction: Option[Action],
  neutralAction: Option[Action],
  negativeAction: Option[Action],
  dismissAction: Option[Action]
) extends Serializable
