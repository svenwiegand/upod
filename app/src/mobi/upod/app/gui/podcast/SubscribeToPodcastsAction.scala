package mobi.upod.app.gui.podcast

import android.content.Context
import com.escalatesoft.subcut.inject.BindingModule
import mobi.upod.app.data.PodcastListItem

private[podcast] class SubscribeToPodcastsAction(podcasts: => IndexedSeq[PodcastListItem])(implicit bindings: BindingModule)
  extends BulkPodcastAction(podcasts) {

  protected def enabled(podcasts: IndexedSeq[PodcastListItem]) =
    podcasts.exists(!_.subscribed)

  protected def processData(context: Context, data: IndexedSeq[PodcastListItem]): Traversable[PodcastListItem] = {
    val unsubscribed = data.filter(!_.subscribed)
    subscriptionService.subscribe(unsubscribed.map(_.uri))
    unsubscribed.map(_.copy(subscribed = true))
  }
}
