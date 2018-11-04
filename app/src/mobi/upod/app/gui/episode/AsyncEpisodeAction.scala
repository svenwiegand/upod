package mobi.upod.app.gui.episode

import mobi.upod.android.app.action.{ActionState, AsyncAction}
import com.escalatesoft.subcut.inject.{BindingModule, Injectable}
import mobi.upod.app.data.EpisodeListItem
import android.content.Context

private[episode] abstract class AsyncEpisodeAction(e: => Option[EpisodeListItem])(implicit val bindingModule: BindingModule)
  extends AsyncAction[EpisodeListItem, EpisodeListItem] with EpisodeActionBase {

  protected def episode = e

  protected def getData(context: Context) = episode.get
}
