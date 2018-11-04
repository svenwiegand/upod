package mobi.upod.app.gui.episode.download

import android.content.Context
import com.escalatesoft.subcut.inject.{BindingModule, Injectable}
import mobi.upod.android.app.action.{ActionState, AsyncAction}
import mobi.upod.app.data.PodcastListItem
import mobi.upod.app.services.download.DownloadService

private[episode] class DeleteRecentlyFinishedPodcastEpisodesAction(
  podcast: => PodcastListItem,
  episodesAvailable: => Boolean)(
  implicit val bindingModule: BindingModule)
  extends AsyncAction[Unit, Unit]
  with Injectable {

  override def state(context: Context): ActionState.ActionState =
    if (episodesAvailable) ActionState.enabled else ActionState.gone

  override protected def getData(context: Context): Unit = ()

  override protected def processData(context: Context, data: Unit): Unit = {
    inject[DownloadService].deleteRecentlyFinished(podcast)
  }
}

