package mobi.upod.app.gui.episode.download

import android.content.Context
import com.escalatesoft.subcut.inject.BindingModule
import mobi.upod.app.data.EpisodeListItem
import mobi.upod.app.gui.episode.EpisodeAction

private[episode] class EpisodeDownloadErrorAction(e: => Option[EpisodeListItem])(implicit bindings: BindingModule)
  extends EpisodeAction(e) {

  protected def enabled(episode: EpisodeListItem) =
    episode.downloadInfo.lastErrorText.isDefined

  def onFired(context: Context) {
    episode.foreach { e =>
      EpisodeDownloadErrorDialogFragment.showIfActivity(context, e)
    }
  }
}

