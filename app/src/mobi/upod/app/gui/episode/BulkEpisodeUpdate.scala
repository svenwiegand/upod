package mobi.upod.app.gui.episode

import mobi.upod.app.data.EpisodeListItem
import android.content.Context

trait BulkEpisodeUpdate extends BulkEpisodeAction {
  protected def updateEpisodes(episodes: Traversable[EpisodeListItem])

  override protected def postProcessData(context: Context, result: Traversable[EpisodeListItem]) {
    updateEpisodes(result)
  }
}
