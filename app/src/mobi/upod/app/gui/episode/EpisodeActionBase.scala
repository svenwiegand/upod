package mobi.upod.app.gui.episode

import android.content.Context
import com.escalatesoft.subcut.inject.{BindingModule, Injectable}
import mobi.upod.android.app.action.{Action, ActionState, AsyncAction}
import mobi.upod.app.data.EpisodeListItem

private[episode] trait EpisodeActionBase extends Action with Injectable {
  protected def episode: Option[EpisodeListItem]

  override def state(context: Context) =
    if (episode.exists(enabled)) ActionState.enabled else ActionState.gone

  protected def enabled(episode: EpisodeListItem): Boolean
}
