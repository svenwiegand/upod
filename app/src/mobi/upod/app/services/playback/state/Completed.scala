package mobi.upod.app.services.playback.state

import com.escalatesoft.subcut.inject.BindingModule
import mobi.upod.app.data.EpisodeListItem
import mobi.upod.app.services.playback.EndPositionChecker
import mobi.upod.media.MediaChapterTable
import mobi.upod.util.Duration._

import scala.util.Try

private[playback] class Completed(
    protected val initialEpisode: EpisodeListItem,
    val chapters: MediaChapterTable)(
    implicit
    stateMachine: StateMachine,
    binding: BindingModule)
  extends StateWithUpdatableEpisode
  with Seekable
  with Resumable
  with Stoppable
  with StateWithPlaybackPosition {

  private val reachedEnd = EndPositionChecker.isAtEndPosition(episode, player.getCurrentPosition)

  override protected[state] def onEnterState() {
    super.onEnterState()
    reloadEpisodeFromDatabase {
      if (!reachedEnd && !episode.downloadInfo.complete && player.playsLocalFiles)
        stream()
      else
        finished()
    }
  }

  private def stream() {

    def seekToSafePosition() {
      val position = if (player.getCurrentPosition > 1.second) player.getCurrentPosition - 1.second else 0
      player.seekTo(position, true)
      updateProgress(true)
    }

    log.info(s"playback completed at position ${player.getCurrentPosition} of ${episode.media.duration}; too slow buffering?")
    seekToSafePosition()
    player.reset()
    transitionToState(new LocalBuffering(episode))
  }

  private def finished() {
    log.info(s"finished playback at position ${Try(player.getCurrentPosition).getOrElse(-1)} of ${episode.media.duration}")
    markFinished()
    fire(_.onEpisodeCompleted(episode))
  }
}
