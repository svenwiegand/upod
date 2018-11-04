package mobi.upod.app.gui.episode.playlist

import android.content.Context
import com.escalatesoft.subcut.inject.BindingModule
import mobi.upod.app.data.EpisodeListItem
import mobi.upod.app.gui.episode.AsyncEpisodeAction
import mobi.upod.app.services.cast.MediaRouteService
import mobi.upod.app.services.playback.PlaybackService

private[episode] abstract class AbstractPlayEpisodeAction(episode: => Option[EpisodeListItem])(implicit bindings: BindingModule)
  extends AsyncEpisodeAction(episode) {

  protected lazy val playbackService = inject[PlaybackService]
  protected lazy val mediaRouteService = inject[MediaRouteService]

  private def thisEpisodeIsPlaying =
    playbackService.playingEpisode.map(_.id) == this.episode.map(_.id) && playbackService.isPlaying

  protected def enabled(episode: EpisodeListItem) =
    !thisEpisodeIsPlaying && isAdequatePlaybackAction(episode)

  protected def isAdequatePlaybackAction(episode: EpisodeListItem): Boolean

  override protected def shouldExecute(context: Context, data: EpisodeListItem): Boolean =
    !playbackService.showPredictablePlaybackError(Some(context), Some(data))

  protected def processData(context: Context, data: EpisodeListItem) = {
    playbackService.play(data, Some(context))
    data.copy(library = true, playbackInfo = data.playbackInfo.copy(listPosition = Some(0)))
  }
}
