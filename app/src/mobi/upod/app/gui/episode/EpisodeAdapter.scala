package mobi.upod.app.gui.episode

import android.widget.ListAdapter
import android.view.View
import mobi.upod.app.data.EpisodeListItem
import mobi.upod.app.storage.EpisodeListHash

trait EpisodeAdapter extends ListAdapter {
  /** An optional function determining whether isEmpty should be force to return true or not
    * to ensure that the list view is displayed (e.g. if there are headers available which should always be shown)
    */
  var forceNotEmpty: Option[Unit => Boolean] = None

  def calculateListHash: EpisodeListHash =
    EpisodeListHash(episodes.size, episodes.map(_.id).sum)

  def setItems(items: IndexedSeq[EpisodeListItem])

  def update(episodes: Traversable[EpisodeListItem], notifyChanged: Boolean = true)

  def remove(ids: Set[Long])

  def episodeByView(view: View): Option[EpisodeListItem]

  def episodes: IndexedSeq[EpisodeListItem]

  def getEpisode(position: Int): Option[EpisodeListItem]

  def getEpisodePosition(episode: EpisodeListItem): Option[Int]

  abstract override def isEmpty: Boolean =
    if (forceNotEmpty.exists(_(()) == true)) false else super.isEmpty
}
