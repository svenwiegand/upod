package mobi.upod.app.gui.podcast

import android.content.Context
import com.escalatesoft.subcut.inject.{Injectable, BindingModule}
import mobi.upod.android.app.action.ActionState._
import mobi.upod.android.app.action.AsyncAction
import mobi.upod.app.data.{PodcastListItem, Podcast}
import mobi.upod.app.storage.PodcastDao

class SubscriptionSettingsAction(podcast: => PodcastListItem)(implicit val bindingModule: BindingModule)
  extends AsyncAction[PodcastListItem, Option[Podcast]]
  with Injectable {

  private lazy val podcastDao = inject[PodcastDao]

  override def state(context: Context): ActionState =
    if (podcast.subscribed) enabled else gone

  protected def getData(context: Context): PodcastListItem = podcast

  protected def processData(context: Context, data: PodcastListItem): Option[Podcast] =
    podcastDao.find(data.id)

  override protected def postProcessData(context: Context, result: Option[Podcast]): Unit = result.foreach { p =>
    SubscriptionSettingsActivity.start(context, p)
  }
}
