package mobi.upod.app.gui.podcast

import android.content.Context
import com.escalatesoft.subcut.inject.BindingModule
import mobi.upod.app.data.PodcastListItem

private[podcast] class DeletePodcastsAction(podcasts: => IndexedSeq[PodcastListItem])(implicit bindings: BindingModule)
  extends BulkPodcastAction(podcasts) {

  protected def enabled(podcasts: IndexedSeq[PodcastListItem]) =
    true

  protected def processData(context: Context, data: IndexedSeq[PodcastListItem]): Traversable[PodcastListItem] = {
    subscriptionService.delete(data.map(_.uri))
    Traversable()
  }
}
