package mobi.upod.android.widget.card

import android.content.Context
import android.view.View
import android.widget.{Button, FrameLayout}
import mobi.upod.android.app.action.Action
import mobi.upod.android.view.ChildViews
import mobi.upod.app.R

class CardView(context: Context) extends FrameLayout(context: Context) with ChildViews {
  private var dismissListener: Option[OnCardDismissListener] = None
  private lazy val titleView = childTextView(R.id.cardTitle)
  protected lazy val contentContainer = childViewGroup(R.id.cardContent)
  private lazy val buttonBar = childViewGroup(R.id.cardButtonBar)

  init()

  protected def init(): Unit = {
    View.inflate(context, R.layout.card, this)
    onInitCard()
  }

  protected def onInitCard(): Unit = {
  }

  def setTitle(title: String): Unit =
    titleView.setText(title)

  def setTitle(resId: Int): Unit =
    titleView.setText(resId)

  def clearContent(): Unit =
    contentContainer.removeAllViews()

  def addContent(view: View): View = {
    contentContainer.addView(view)
    view
  }

  def addContent(layoutId: Int): View = {
    View.inflate(context, layoutId, contentContainer)
    contentContainer.getChildAt(contentContainer.getChildCount - 1)
  }

  def addButtons(buttons: CardButton*): Unit =
    buttons foreach addButton

  def addButton(button: CardButton): Unit = {
    val index = buttonBar.getChildCount
    val btn = inflateButton(button.primary)
    btn.setText(button.text)
    btn.onClick(onButtonClick(index, button.action))
  }

  def resetOnDismissListener(): Unit =
    dismissListener = None

  def setOnDismissListener(listener: OnCardDismissListener): Unit =
    dismissListener = Some(listener)

  private def inflateButton(primary: Boolean): Button = {
    View.inflate(context, if (primary) R.layout.card_button_primary else R.layout.card_button, buttonBar)
    buttonBar.getChildAt(buttonBar.getChildCount - 1).asInstanceOf[Button]
  }

  protected def shouldDismissOnButtonClick(btnIndex: Int): Boolean = true

  protected def onButtonClick(btnIndex: Int, action: Option[Action]): Unit = {
    action.filter(_.isEnabled(context)).foreach(_.fire(context))
    if (shouldDismissOnButtonClick(btnIndex)) {
      onDismiss()
    }
  }

  protected def onDismiss(): Unit =
    dismissListener.foreach(_.onDismiss(this))
}
