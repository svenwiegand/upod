package mobi.upod.app.gui.episode

import android.view.View
import com.escalatesoft.subcut.inject.{Injectable, BindingModule}
import mobi.upod.android.view.Helpers._
import mobi.upod.android.widget._
import mobi.upod.app.R
import mobi.upod.app.data.EpisodeListItem
import mobi.upod.util.Collections._
import scala.collection.mutable.ArrayBuffer

class GroupedEpisodeListItemAdapter
    (initialItems: IndexedSeq[EpisodeListItem], viewHolder: (View) => EpisodeListItemViewHolder)
    (implicit val bindingModule: BindingModule)
  extends GroupedItemAdapter[SimpleHeader, EpisodeListItem](R.layout.simple_list_section_header, R.layout.episode_list_item)
  with EpisodeAdapter with StableIds with Injectable {

  private var _episodes = initialItems

  def episodes = _episodes

  private var _entries = groupItems(_episodes)

  protected def entries: IndexedSeq[Either[SimpleHeader, EpisodeListItem]] = _entries

  protected type ItemViewHolder = EpisodeListItemViewHolder

  protected type HeaderViewHolder = SimpleHeaderViewHolder

  protected def itemId(position: Int) = if (position >= 0 && position < entries.size)
    itemId(entries(position))
  else
    -1

  private def itemId(entry: Either[SimpleHeader, EpisodeListItem]) = entry fold (
    header => header.id,
    item => item.id
  )

  protected def createHeaderViewHolder(view: View) = new SimpleHeaderViewHolder(view)

  protected def createItemViewHolder(view: View) = viewHolder(view)

  def episodeByView(view: View): Option[EpisodeListItem] = view.viewHolder[ViewHolder[_]] match {
    case Some(viewHolder: ItemViewHolder) => viewHolder.episodeListItem
    case _ => None
  }

  def getEpisode(position: Int) = entries(position) fold (
    header => None,
    item => Some(item)
  )

  def getEpisodePosition(episode: EpisodeListItem) =
    _entries.indexWhere(_.fold(_ => false, _.id == episode.id)).validIndex

  private def groupItems(episodes: IndexedSeq[EpisodeListItem]): ArrayBuffer[Either[SimpleHeader, EpisodeListItem]] = {

    def header(episode: EpisodeListItem): Either[SimpleHeader, EpisodeListItem] =
      Left(SimpleTextHeader(episode.podcastInfo.id, episode.podcastInfo.title))

    def episode(episode: EpisodeListItem): Either[SimpleHeader, EpisodeListItem] =
      Right(episode)

    if (episodes.isEmpty) {
      ArrayBuffer()
    } else {
      val head = ArrayBuffer(header(episodes(0)), episode(episodes(0)))
      val tail = episodes zip episodes.tail flatMap { aAndB =>
        val (a, b) = aAndB
        if (a.podcastInfo.id == b.podcastInfo.id) {
          Seq(episode(b))
        } else {
          Seq(header(b), episode(b))
        }
      }
      head ++ tail
    }
  }

  private def update(episode: EpisodeListItem) {
    val index = _entries.indexWhere(_.fold(_ => false, _.id == episode.id)).validIndex
    index.foreach(_entries.update(_, Right(episode)))
  }

  def update(updatedEpisodes: Traversable[EpisodeListItem], notifyChanged: Boolean = true) {
    updatedEpisodes.foreach(update)
    if (notifyChanged) {
      notifyDataSetChanged()
    }
  }

  def remove(ids: Set[Long]) {
    _episodes = _episodes filterNot { episode => ids contains episode.id }
    _entries = groupItems(_episodes)
    notifyDataSetChanged()
  }

  def setItems(items: IndexedSeq[EpisodeListItem]) {
    _episodes = items
    _entries = groupItems(_episodes)
    notifyDataSetChanged()
  }
}
