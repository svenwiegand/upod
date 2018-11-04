package mobi.upod.app.gui.episode.online

import android.content.Context
import com.escalatesoft.subcut.inject.{BindingModule, Injectable}
import mobi.upod.android.app.action.AsyncAction
import mobi.upod.app.services.EpisodeService

private[episode]  class AddUnfinishedEpisodesToLibraryAction(podcastId: => Long)(implicit val bindingModule: BindingModule)
  extends AsyncAction[Unit, Unit]
  with Injectable {

  override protected def getData(context: Context): Unit = ()

  override protected def processData(context: Context, data: Unit): Unit = {
    inject[EpisodeService].addUnfinishedToLibrary(podcastId)
  }
}
