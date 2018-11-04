package mobi.upod.android.view

import android.graphics.Rect
import android.view.{View, ViewGroup, ViewParent}
import android.widget.CompoundButton
import mobi.upod.android.app.action.Action
import mobi.upod.android.os.Runnable
import mobi.upod.app.R

object Helpers {

  implicit class RichView(val view: View) extends AnyVal with ChildViews {

    def onClick(handle: => Unit) {
      view.setOnClickListener(ClickListener(handle))
    }

    def onClick(handle: View => Unit) {
      view.setOnClickListener(ClickListener(handle))
    }

    def setClickAction(action: Action): Unit = {
      onClick(if (action.isEnabled(view.getContext)) action.fire(view.getContext))
      view.setEnabled(!action.isDisabled(view.getContext))
      show(!action.isGone(view.getContext))
    }

    def onLongClick(handle: => Unit): Unit = {
      view.setOnLongClickListener(LongClickListener(handle))
    }

    def onLongClick(handle: View => Unit): Unit = {
      view.setOnLongClickListener(LongClickListener(handle))
    }

    def findViewById(id: Int) = view.findViewById(id)

    def show(show: Boolean = true) {
      view.setVisibility(if (show) View.VISIBLE else View.GONE)
    }

    def hide() {
      show(false)
    }

    def makeInvisible(invisible: Boolean = true): Unit = {
      view.setVisibility(if (invisible) View.INVISIBLE else View.VISIBLE)
    }

    def viewHolder_=(viewHolder: AnyRef) {
      view.setTag(R.id.view_holder, viewHolder)
    }

    def viewHolder[A]: Option[A] = Option(view.getTag(R.id.view_holder)) map { _.asInstanceOf[A] }

    def post(action: => Unit): Unit = view.post(Runnable(action))

    def postDelayed(delayMillis: Long, action: => Unit): Unit = view.postDelayed(Runnable(action), delayMillis)

    def firstParentWhere(matches: View => Boolean): Option[View] = {

      def recurse(parent: ViewParent): Option[View] = parent match {
        case null => None
        case p: View if matches(p) => Some(p)
        case p => recurse(p.getParent)
      }

      recurse(view.getParent)
    }

    def getGlobalVisibleRect(): Option[Rect] = {
      val rect = new Rect
      if (view.getGlobalVisibleRect(rect))
        Some(rect)
      else
        None
    }

    def getRelativeVisibleRect(reference: Option[Rect]): Option[Rect] = (reference, getGlobalVisibleRect()) match {
      case (Some(refRect), Some(rect)) =>
        rect.offset(-refRect.left, -refRect.top)
        Some(rect)
      case _ => None
    }
    
    def getRelativeVisibleRect(other: View): Option[Rect] =
      getRelativeVisibleRect(RichView(other).getGlobalVisibleRect())
  }

  implicit class RichViewGroup(val group: ViewGroup) extends AnyVal {

    def childViews: Iterator[View] = new ChildViewIterator(group)

    def logViewHierarchy(log: String => Unit): Unit = {

      def logView(view: View, indent: String = "") {
        log(s"$indent $view")
        view match {
          case group: ViewGroup =>
            group.childViews.foreach(logView(_, indent + "-"))
          case _ =>
        }
      }

      logView(group)
    }

    def firstChildWhere(matches: View => Boolean): Option[View] = {
    
      def visitChild(child: View, matches: View => Boolean): Option[View] = {
        if (matches(child))
          Some(child)
        else {
          child match {
            case g: ViewGroup => g.firstChildWhere(matches)
            case v => None
          }
        }
      }

      
      childViews.foreach { view =>
        visitChild(view, matches) match {
          case Some(v) =>
            return Some(v)
          case _ =>
        }
      }
      None
    }
  }

  implicit class RichCompoundButton(val button: CompoundButton) extends AnyVal {

    def onCheckedChanged(handle: Boolean => Unit): Unit =
      button.setOnCheckedChangeListener(CompoundButtonCheckedChangeListener(handle))
  }
}
