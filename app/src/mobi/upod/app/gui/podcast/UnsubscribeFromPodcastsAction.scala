package mobi.upod.app.gui.podcast

import android.content.Context
import com.escalatesoft.subcut.inject.BindingModule
import mobi.upod.app.data.PodcastListItem

private[podcast] class UnsubscribeFromPodcastsAction(podcasts: => IndexedSeq[PodcastListItem])(implicit bindings: BindingModule)
  extends BulkPodcastAction(podcasts) {

  protected def enabled(podcasts: IndexedSeq[PodcastListItem]) =
    podcasts.exists(_.subscribed)

  protected def processData(context: Context, data: IndexedSeq[PodcastListItem]): Traversable[PodcastListItem] = {
    val subscribed = data.filter(_.subscribed)
    subscriptionService.unsubscribe(subscribed.map(_.uri))
    subscribed.map(_.copy(subscribed = false))
  }
}
