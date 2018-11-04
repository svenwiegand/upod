package mobi.upod.app.services.download

import android.content.Context
import com.escalatesoft.subcut.inject.Injectable
import mobi.upod.android.app.action.{ActionState, Action}
import com.escalatesoft.subcut.inject.BindingModule

class DownloadListAction(implicit val bindingModule: BindingModule) extends Action with Injectable {
  private lazy val downloadService = inject[DownloadService]

  override def state(context: Context): ActionState.ActionState =
    if (!downloadService.downloadingEpisode.isDefined) ActionState.enabled else ActionState.gone

  def onFired(context: Context) {
    downloadService.downloadQueue()
  }
}
