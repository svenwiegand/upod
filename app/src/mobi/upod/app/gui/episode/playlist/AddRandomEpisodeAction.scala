package mobi.upod.app.gui.episode.playlist

import android.content.Context
import com.escalatesoft.subcut.inject.{Injectable, BindingModule}
import mobi.upod.app.data.EpisodeListItem
import mobi.upod.app.gui.episode.{EpisodeUpdate, AsyncEpisodeAction}
import mobi.upod.app.services.playback.PlaybackService
import mobi.upod.android.app.action.{Action, AsyncAction}
import mobi.upod.android.os.AsyncTask

final class AddRandomEpisodeAction(implicit val bindingModule: BindingModule) extends Action with Injectable {
  private lazy val playService = inject[PlaybackService]

  override def onFired(context: Context): Unit =
    AsyncTask.execute(playService.addRandomEpisode())
}
