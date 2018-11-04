package mobi.upod.app.gui.episode.library

import android.view.View
import com.escalatesoft.subcut.inject.BindingModule
import mobi.upod.android.app.action.Action
import mobi.upod.app.R
import mobi.upod.app.gui.episode._

class StarredLibraryEpisodeListItemViewHolder
  (view: View, config: EpisodeListItemViewHolderConfiguration)
  (implicit bindingModule: BindingModule)
  extends LibraryEpisodeListItemViewHolder(view, config) {

  override protected def createActions: Map[Int, Action] = super.createActions ++ Map(
    R.id.action_mark_unfinished -> new MarkEpisodeUnfinishedAction(episodeListItem) with EpisodeAdapterUpdate,
    R.id.action_mark_finished -> new MarkEpisodeFinishedAction(episodeListItem) with EpisodeAdapterUpdate
  )
}
