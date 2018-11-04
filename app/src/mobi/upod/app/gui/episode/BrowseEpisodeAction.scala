package mobi.upod.app.gui.episode

import android.content.{Context, Intent}
import android.net.Uri
import mobi.upod.android.app.action.ActionState.ActionState
import mobi.upod.android.app.action.{Action, ActionState}
import mobi.upod.app.data.EpisodeBase

class BrowseEpisodeAction(episode: => Option[EpisodeBase]) extends Action {

  private def showNotesLink = episode.flatMap(_.showNotesLink.map(Uri.parse))

  override def state(context: Context): ActionState =
    if (showNotesLink.isDefined) ActionState.enabled else ActionState.gone

  def onFired(context: Context): Unit = showNotesLink foreach { link =>
    context.startActivity(new Intent(Intent.ACTION_VIEW, link))
  }
}
