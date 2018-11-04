package mobi.upod.android.app.action

import ActionState._
import android.content.Context

trait Action extends Serializable {

  def fire(context: Context): Unit =
    onFired(context)

  def onFired(context: Context): Unit

  def state(context: Context): ActionState = enabled

  final def isEnabled(context: Context): Boolean = state(context) == enabled

  final def isDisabled(context: Context): Boolean = state(context) == disabled

  final def isGone(context: Context): Boolean = state(context) == gone

  final def isVisible(context: Context): Boolean = !isGone(context)
}

object Action {
  def apply(handle: Context => Unit): Action = new Action {
    def onFired(context: Context) = handle(context)
  }

  def apply(handle: => Unit): Action = apply(_ => handle)

  def apply(handle: Context => Unit, enabled: Context => Boolean, disabledState: ActionState): Action = new Action {

    override def state(context: Context): ActionState =
      if (enabled(context)) ActionState.enabled else disabledState

    override def onFired(context: Context): Unit = handle(context)
  }

  def apply(handle: => Unit, enabled: => Boolean, disabledState: ActionState): Action =
    apply(_ => handle, _ => enabled, disabledState)
}