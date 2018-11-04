package mobi.upod.android.widget

import android.content.Context
import android.support.v7.widget.PopupMenu
import android.util.AttributeSet
import android.view.{Menu, MenuItem, View}
import android.widget.LinearLayout
import mobi.upod.android.view.ChildViews
import mobi.upod.app.R

class ItemContextMenu(context: Context, attributes: AttributeSet)
  extends LinearLayout(context, attributes)
  with ChildViews
  with PopupMenu.OnMenuItemClickListener {

  setOrientation(LinearLayout.VERTICAL)
  View.inflate(getContext, R.layout.item_context_menu, this)

  private val primaryActionButton = childImageView(R.id.primaryItemAction)
  private val overflowButton = childAs[View](R.id.itemOverflowMenu)
  overflowButton.onClick(showPopupMenu())

  private val popupMenu = new PopupMenu(getContext, overflowButton)
  popupMenu.setOnMenuItemClickListener(this)

  private var _listener: Option[ContextMenuListener] = None

  def listener_=(listener: ContextMenuListener) {
    _listener = Some(listener)
    listener.onCreateMenu(popupMenu.getMenu, this)
  }

  def listener: Option[ContextMenuListener] = _listener

  var primaryActionChooser: PrimaryActionChooser = new SimplePrimaryActionChooser

  def invalidateMenu() {
    _listener match {
      case Some(listener) =>
        listener.onPrepareMenu(popupMenu.getMenu)
        val primaryActionItem = getAndHidePrimaryActionItem(popupMenu.getMenu)
        initPrimaryActionButton(primaryActionItem)
        initOverflowMenu(popupMenu.getMenu)
      case None =>
    }
  }

  private def getAndHidePrimaryActionItem(menu: Menu, index: Int = 0): Option[MenuItem] = {
    primaryActionChooser.choosePrimaryAction(menu).map { primaryActionItem =>
      primaryActionItem.setVisible(false)
      primaryActionItem
    }
  }

  private def initPrimaryActionButton(menuItem: Option[MenuItem]) {
    menuItem match {
      case Some(item) =>
        primaryActionButton.setImageDrawable(item.getIcon)
        primaryActionButton.show()
        primaryActionButton.onClick(onPrimaryActionClick(item))
        primaryActionButton.onLongClick(onPrimaryActionTip(item))
        _listener.foreach(_.onPreparePrimaryAction(item, primaryActionButton))
      case None =>
        primaryActionButton.hide()
    }
  }

  private def initOverflowMenu(menu: Menu) {
    overflowButton.setVisibility(if (menu.hasVisibleItems) View.VISIBLE else View.INVISIBLE)
  }

  private def showPopupMenu() {
    popupMenu.show()
  }

  private def onPrimaryActionClick(item: MenuItem) {
    onMenuItemClick(item)
  }

  def onMenuItemClick(item: MenuItem): Boolean = listener match {
    case Some(l) => l.onContextItemSelected(item)
    case _ => false
  }

  private def onPrimaryActionTip(item: MenuItem) {
    Toast.showFor(primaryActionButton, item.getTitle)
  }
}