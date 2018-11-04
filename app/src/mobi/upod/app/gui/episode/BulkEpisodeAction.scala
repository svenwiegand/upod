package mobi.upod.app.gui.episode

import com.escalatesoft.subcut.inject.{Injectable, BindingModule}
import mobi.upod.android.app.action.{ActionState, AsyncAction}
import mobi.upod.app.services.EpisodeService
import mobi.upod.app.data.EpisodeListItem
import android.content.Context

private[episode] abstract class BulkEpisodeAction(episodes: => IndexedSeq[EpisodeListItem])(implicit val bindingModule: BindingModule)
  extends AsyncAction[IndexedSeq[EpisodeListItem], Traversable[EpisodeListItem]]
  with Injectable {

  protected lazy val episodeService = inject[EpisodeService]

  override def state(context: Context): ActionState.ActionState =
    if (!episodes.isEmpty && enabled(episodes)) ActionState.enabled else ActionState.gone

  protected def enabled(episodes: IndexedSeq[EpisodeListItem]): Boolean

  protected def getData(context: Context): IndexedSeq[EpisodeListItem] = episodes
}
