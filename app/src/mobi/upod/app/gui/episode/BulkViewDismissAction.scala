package mobi.upod.app.gui.episode

import android.content.Context
import android.widget.ListView
import mobi.upod.android.app.action.AsyncAction
import mobi.upod.android.util.CollectionConverters._
import mobi.upod.app.data.EpisodeListItem

trait BulkViewDismissAction extends AsyncAction[IndexedSeq[EpisodeListItem], Traversable[EpisodeListItem]] {
  def dismissController: Option[EpisodeDismissController]
  def listView: ListView

  override protected def preProcessData(context: Context, data: IndexedSeq[EpisodeListItem]) {
    val episodes = data
    listView.getCheckedItemPositions foreach { listView.setItemChecked(_, false) }
    dismissController foreach { controller =>
      episodes foreach { episode => controller.dismiss(episode.id) }
      controller.commitPendingDismissesIfApplicable()
    }
  }
}
