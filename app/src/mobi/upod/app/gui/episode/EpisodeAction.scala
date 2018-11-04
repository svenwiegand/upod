package mobi.upod.app.gui.episode

import com.escalatesoft.subcut.inject.BindingModule
import mobi.upod.android.app.action.Action
import mobi.upod.app.data.EpisodeListItem

private[episode] abstract class EpisodeAction(e: => Option[EpisodeListItem])(implicit val bindingModule: BindingModule)
  extends Action with EpisodeActionBase {

  protected def episode = e
}
