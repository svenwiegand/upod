package mobi.upod.app.gui.episode

import android.content.Intent
import mobi.upod.app.data.EpisodeListItem
import mobi.upod.app.gui.ListViewCloser

trait EpisodeListHolder extends ListViewCloser {

  def enableEpisodeActions: Boolean

  def checkClickedEpisode: Boolean

  def checkedEpisode: Option[EpisodeListItem]

  def openEpisode(episodeListItem: EpisodeListItem, navigationItemId: Long, viewModeId: Int, prepareIntent: Intent => Unit)
}
