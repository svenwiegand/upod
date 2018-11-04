package mobi.upod.app.gui.announcement

import android.content.Context
import com.crashlytics.android.answers.{CustomEvent, Answers}
import com.escalatesoft.subcut.inject.{BindingModule, Injectable}
import mobi.upod.android.app.action.{Action, BrowseAction}
import mobi.upod.android.view.cards.CardHeader
import mobi.upod.android.widget.card.{CardButton, CardView, TextCardView}
import mobi.upod.app.services.AnnouncementService

class AnnouncementCardHeader(implicit val bindingModule: BindingModule)
  extends CardHeader
  with Injectable {

  private val announcementService = inject[AnnouncementService]

  override def shouldShow: Boolean =
    announcementService.currentAnnouncement.isDefined

  override def create(context: Context): CardView = {
    val announcement = announcementService.currentAnnouncement.get
    val primaryButton: Option[CardButton] = announcement.primaryButton.map { label =>
      CardButton(label, announcement.primaryUrl.map(new BrowseAction(_)), true)
    }
    val secondaryButton: Option[CardButton] = announcement.secondaryButton.map { label =>
      CardButton(label, announcement.secondyrUrl.map(new BrowseAction(_)), false)
    }
    val buttons = Seq(primaryButton, secondaryButton).filter(_.isDefined).map(_.get)
    new AnnouncementCardView(context, announcement.id, announcement.title, announcement.message, buttons)
  }

  override def onDismiss(): Unit = () // the card view below does the work on dismiss

  private class AnnouncementCardView(context: Context, id: Long, title: String, message: String, buttons: Seq[CardButton])
    extends TextCardView(context, title, message, buttons: _*) {

    logShowEvent()

    override protected def shouldDismissOnButtonClick(btnIndex: Int): Boolean =
      buttons(btnIndex).action.isEmpty

    override protected def onDismiss(): Unit = {
      super.onDismiss()
      announcementService.dismissAnnouncement(id)
    }

    override protected def onButtonClick(btnIndex: Int, action: Option[Action]): Unit = {
      logButtonEvent(btnIndex)
      super.onButtonClick(btnIndex, action)
    }

    private def logShowEvent(): Unit = {
      inject[Answers].logCustom(new CustomEvent(s"Announcement $id").putCustomAttribute("announcement", id).putCustomAttribute("title", title))
    }

    private def logButtonEvent(btnIndex: Int): Unit = {
      inject[Answers].logCustom(new CustomEvent(s"Announcement $id Button $btnIndex")
        .putCustomAttribute("announcement", id)
        .putCustomAttribute("title", title)
        .putCustomAttribute("button", btnIndex)
        .putCustomAttribute("action", buttons(btnIndex).text))
    }
  }
}