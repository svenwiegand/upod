package mobi.upod.app.services.playback

import com.escalatesoft.subcut.inject.{Injectable, BindingModule}
import mobi.upod.android.app.action.{ActionState, Action}
import android.content.Context

private[playback] abstract class PlaybackAction(implicit val bindingModule: BindingModule)
  extends Action
  with Injectable {

  protected lazy val playbackService = inject[PlaybackService]

  override def state(context: Context) =
    if (enabled) ActionState.enabled else ActionState.gone

  protected def enabled: Boolean
}
