package mobi.upod.app.gui.episode.playlist

import com.escalatesoft.subcut.inject.BindingModule
import mobi.upod.app.data.{EpisodeBaseWithPlaybackInfo, EpisodeListItem}
import mobi.upod.app.gui.episode.{EpisodeAction, AsyncEpisodeAction}
import mobi.upod.app.services.playback.PlaybackService
import android.content.Context

private[episode] class PauseEpisodeAction(episode: => Option[EpisodeListItem])(implicit bindings: BindingModule)
  extends EpisodeAction(episode) {

  private lazy val playbackService = inject[PlaybackService]

  protected def enabled(episode: EpisodeListItem) =
    playbackService.playingEpisode.exists(_.id == episode.id) && playbackService.isPlaying

  def onFired(context: Context) {
    playbackService.pause()
  }
}
