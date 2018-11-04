package mobi.upod.app.gui.episode.playlist

import com.escalatesoft.subcut.inject.BindingModule
import mobi.upod.app.data.EpisodeListItem
import mobi.upod.app.gui.episode.{EpisodeUpdate, AsyncEpisodeAction}
import mobi.upod.app.services.playback.PlaybackService
import android.content.Context

private[episode] abstract class PlayEpisodeNextAction(episode: => Option[EpisodeListItem])(implicit bindings: BindingModule)
  extends AsyncEpisodeAction(episode) with EpisodeUpdate {
  private lazy val playbackService = inject[PlaybackService]

  private def thisEpisodeIsPlaying =
    playbackService.playingEpisode.map(_.id) == this.episode.map(_.id) && playbackService.isPlaying

  protected def enabled(episode: EpisodeListItem) =
    !thisEpisodeIsPlaying

  protected def processData(context: Context, data: EpisodeListItem) = {
    playbackService.playNext(data)
    data.copy(library = true, playbackInfo = data.playbackInfo.copy(listPosition = Some(1)))
  }
}
