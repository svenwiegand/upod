package mobi.upod.app.gui.chapters

import android.content.{Context, Intent}
import android.net.Uri
import mobi.upod.android.app.action.ActionState.ActionState
import mobi.upod.android.app.action.{Action, ActionState}
import mobi.upod.media.ChapterWebLink

class OpenChapterLinkAction(link: => Option[ChapterWebLink]) extends Action {

  override def state(context: Context): ActionState =
    if (link.isDefined) ActionState.enabled else ActionState.gone

  override def onFired(context: Context): Unit = link foreach { l =>
    context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(l.url)))
  }
}