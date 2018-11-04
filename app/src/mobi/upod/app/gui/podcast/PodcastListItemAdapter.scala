package mobi.upod.app.gui.podcast

import android.view.View
import com.escalatesoft.subcut.inject.{Injectable, BindingModule}
import mobi.upod.android.widget.{MutableItemAdapter, StableIds}
import mobi.upod.app.data.PodcastListItem
import mobi.upod.app.storage.ImageSize

class PodcastListItemAdapter(
    initialItems: IndexedSeq[PodcastListItem],
    itemLayoutId: Int,
    imageSize: ImageSize,
    grid: Boolean,
    slowLoadingImages: Boolean = false)(
    implicit val bindingModule: BindingModule)
  extends MutableItemAdapter[PodcastListItem](itemLayoutId, initialItems)
  with StableIds
  with Injectable {

  protected type ItemViewHolder = PodcastListItemViewHolder

  protected def createViewHolder(view: View) = new PodcastListItemViewHolder(view, imageSize, grid, slowLoadingImages)

  protected def itemId(item: PodcastListItem): Long = item.id

  def update(podcasts: Traversable[PodcastListItem]): Unit =
    super.updateItems(podcasts, _.id == _.id)
}
