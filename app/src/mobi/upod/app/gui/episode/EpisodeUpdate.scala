package mobi.upod.app.gui.episode

import mobi.upod.app.data.EpisodeListItem
import android.content.Context

trait EpisodeUpdate extends AsyncEpisodeAction {
  protected def updateEpisode(episode: EpisodeListItem)

  override protected def postProcessData(context: Context, result: EpisodeListItem) {
    updateEpisode(result)
  }
}
