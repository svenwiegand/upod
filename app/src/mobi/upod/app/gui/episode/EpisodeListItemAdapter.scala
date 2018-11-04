package mobi.upod.app.gui.episode

import android.view.View
import com.escalatesoft.subcut.inject.{Injectable, BindingModule}
import mobi.upod.android.view.Helpers._
import mobi.upod.android.widget.MutableItemAdapter
import mobi.upod.app.{R}
import mobi.upod.util.Collections._
import mobi.upod.app.data.EpisodeListItem

class EpisodeListItemAdapter
  (initialItems: IndexedSeq[EpisodeListItem], viewHolder: (View) => EpisodeListItemViewHolder)
  (implicit val bindingModule: BindingModule)
  extends MutableItemAdapter[EpisodeListItem](R.layout.episode_list_item, initialItems)
  with EpisodeAdapter with Injectable {

  protected type ItemViewHolder = EpisodeListItemViewHolder

  protected def itemId(item: EpisodeListItem): Long = item.id

  def episodeByView(view: View): Option[EpisodeListItem] = view.viewHolder[ItemViewHolder] flatMap { _.episodeListItem }

  def episodes = items

  def getEpisode(position: Int) = Some(episodes(position))

  def getEpisodePosition(episode: EpisodeListItem) = episodes.indexWhere(_.id == episode.id).validIndex

  protected def createViewHolder(view: View) = viewHolder(view)

  def update(episodes: Traversable[EpisodeListItem], notifyChanged: Boolean = true) {
    updateItems(episodes, _.id == _.id, notifyChanged)
  }
}
