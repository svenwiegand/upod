package mobi.upod.android.widget

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.view.View
import mobi.upod.android.app.action.{Action, ActionState}
import mobi.upod.android.view.Helpers._
import mobi.upod.app.R

trait AbstractActionButton extends View {
  protected def context: Context
  protected def attrs: AttributeSet

  private var _action: Option[Action] = None
  private val invisibleWhenGone: Boolean = {
    val a: TypedArray = context.obtainStyledAttributes(attrs, R.styleable.ActionButton)
    val f = a.getBoolean(R.styleable.ActionButton_invisibleWhenGone, false)
    a.recycle()
    f
  }

  this.onClick(handleClick())
  this.onLongClick(showTooltip())

  def setAction(action: Action): Unit = {
    _action = Some(action)
    invalidateAction()
  }

  def withAction(action: Action): AbstractActionButton = {
    setAction(action)
    this
  }

  def invalidateAction(): Unit = {
    _action.map(_.state(getContext)).getOrElse(ActionState.gone) match {
      case ActionState.gone if invisibleWhenGone =>
        makeInvisible()
      case ActionState.gone =>
        hide()
      case ActionState.disabled =>
        setEnabled(false)
      case ActionState.enabled =>
        show()
    }
  }

  protected def makeInvisible(): Unit =
    setVisibility(View.INVISIBLE)

  protected def hide(): Unit =
    setVisibility(View.GONE)

  protected def show(): Unit =
    setVisibility(View.VISIBLE)

  private def handleClick() {
    _action.foreach { action =>
      if (action.isEnabled(getContext)) {
        action.fire(getContext)
      }
    }
  }

  protected def showTooltip(): Unit = getContentDescription match {
    case null => // ignore
    case description => Toast.showFor(this, description)
  }
}
