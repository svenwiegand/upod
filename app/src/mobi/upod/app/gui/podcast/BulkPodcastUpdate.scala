package mobi.upod.app.gui.podcast

import android.content.Context
import mobi.upod.app.data.PodcastListItem

trait BulkPodcastUpdate extends BulkPodcastAction {
  protected def updatePodcasts(episodes: Traversable[PodcastListItem])

  override protected def postProcessData(context: Context, result: Traversable[PodcastListItem]) {
    updatePodcasts(result)
  }
}
