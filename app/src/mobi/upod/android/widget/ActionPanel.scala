package mobi.upod.android.widget

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup.LayoutParams
import android.view.{View, Menu, MenuItem, ViewGroup}
import android.widget.{Button, LinearLayout, PopupMenu}
import mobi.upod.android.content.Theme.context2Theme
import mobi.upod.android.view.{Tintable, ChildViews, DisplayMetrics}
import mobi.upod.app.R

class ActionPanel(context: Context, attributes: AttributeSet)
  extends LinearLayout(context, attributes)
  with ChildViews {

  private val menu: Menu = new PopupMenu(context, null).getMenu
  private val primaryActionButtonId = findPrimaryActionButtonId
  private lazy val primaryActionButton = getParent.asInstanceOf[ViewGroup].optionalChildAs[FloatingActionButton](primaryActionButtonId)
  private implicit val displayMetrics = new DisplayMetrics(context)

  var primaryActionChooser: PrimaryActionChooser = new SimplePrimaryActionChooser

  private var _listener: Option[ContextMenuListener] = None

  init()

  private def findPrimaryActionButtonId: Int = {
    val attrs = context.obtainStyledAttributes(attributes, R.styleable.ActionPanel)
    val id = attrs.getResourceId(R.styleable.ActionPanel_primaryActionButton, 0)
    attrs.recycle()
    id
  }

  def listener_=(listener: ContextMenuListener): Unit =
    _listener = Some(listener)

  def listener: Option[ContextMenuListener] = _listener

  private def init(): Unit = {
    setOrientation(LinearLayout.VERTICAL)
  }

  def invalidateActions(): Unit = {
    _listener match {
      case Some(listener) =>
        menu.clear()
        listener.onCreateMenu(menu, this)
        listener.onPrepareMenu(menu)
        primaryActionButton foreach { btn =>
          val primaryActionItem = getAndHidePrimaryActionItem(menu)
          initPrimaryActionButton(btn, primaryActionItem)
        }
        initActions(menu)
      case None =>
    }
  }

  private def getAndHidePrimaryActionItem(menu: Menu, index: Int = 0): Option[MenuItem] = {
    primaryActionChooser.choosePrimaryAction(menu).map { primaryActionItem =>
      primaryActionItem.setVisible(false)
      primaryActionItem
    }
  }

  private def initPrimaryActionButton(button: FloatingActionButton, menuItem: Option[MenuItem]) {
    menuItem match {
      case Some(item) =>
        val icon = item.getIcon
        Tintable.tint(icon, 0xffffffff)
        button.setImageDrawable(icon)
        button.show()
        button.onClick(onPrimaryActionClick(item))
        button.setContentDescription(item.getTitle)
        _listener.foreach(_.onPreparePrimaryAction(item, button))
      case None =>
        button.hide()
    }
  }

  private def initActions(menu: Menu): Unit = {

    def addAction(item: MenuItem): Unit = {
      val button = View.inflate(context, R.layout.action_panel_button, null).asInstanceOf[Button]
      button.onClick(onMenuItemClick(item))
      val icon = item.getIcon
      icon.setBounds(0, 0, icon.getIntrinsicWidth - 1, icon.getIntrinsicHeight - 1)
      button.setCompoundDrawables(icon, null, null, null)
      button.setText(item.getTitle)
      Tintable.tint(button, context.getThemeColor(R.attr.textColorPrimary))
      addView(button, LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
    }

    def initAction(item: MenuItem): Unit = if (item.isVisible && item.getIcon != null) {
      addAction(item)
    }

    removeAllViews()
    for (i <- 0 until menu.size) {
      initAction(menu.getItem(i))
    }
  }

  private def onPrimaryActionClick(item: MenuItem): Unit = {
    onMenuItemClick(item)
  }

  def onMenuItemClick(item: MenuItem): Unit =
    listener foreach (_.onContextItemSelected(item))
}
