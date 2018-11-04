package mobi.upod.app.gui.episode

import mobi.upod.android.app.action.{AsyncAction}
import mobi.upod.app.data.EpisodeListItem
import android.content.Context

trait ViewDismissAction extends AsyncAction[EpisodeListItem, EpisodeListItem] {
  def dismissController: Option[EpisodeDismissController]

  override protected def preProcessData(context: Context, data: EpisodeListItem) {
    dismissController foreach { controller =>
      controller.dismiss(data.id)
      controller.commitPendingDismissesIfApplicable()
    }
  }
}
