package mobi.upod.app.gui.podcast

import android.content.Context
import com.escalatesoft.subcut.inject.{Injectable, BindingModule}
import mobi.upod.android.app.action.{ActionState, AsyncAction}
import mobi.upod.app.data.PodcastListItem
import mobi.upod.app.services.subscription.SubscriptionService

private[podcast] abstract class BulkPodcastAction(podcasts: => IndexedSeq[PodcastListItem])(implicit val bindingModule: BindingModule)
  extends AsyncAction[IndexedSeq[PodcastListItem], Traversable[PodcastListItem]]
  with Injectable {

  protected lazy val subscriptionService = inject[SubscriptionService]

  override def state(context: Context): ActionState.ActionState =
    if (!podcasts.isEmpty && enabled(podcasts)) ActionState.enabled else ActionState.gone

  protected def enabled(podcasts: IndexedSeq[PodcastListItem]): Boolean

  protected def getData(context: Context): IndexedSeq[PodcastListItem] = podcasts

}
