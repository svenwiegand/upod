package mobi.upod.app.gui

import com.escalatesoft.subcut.inject.Injectable
import mobi.upod.android.view.cards.{CardHeader, CardHeaders}
import mobi.upod.app.gui.announcement.AnnouncementCardHeader

trait AnnouncementCardHeaders extends CardHeaders with Injectable {

  override protected def createCardHeaders: Seq[CardHeader] =
    new AnnouncementCardHeader +: super.createCardHeaders
}