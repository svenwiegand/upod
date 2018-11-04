package mobi.upod.app.services.playback.state

import mobi.upod.app.data.EpisodeListItem
import mobi.upod.app.services.playback.{RemotePlaybackState, PlaybackService, EndPositionChecker}
import mobi.upod.android.os.AsyncTask

private[playback] trait Playable extends PlaybackState {
  private lazy val playService = inject[PlaybackService]

  def play(episode: EpisodeListItem) {
    val e = resetPlaybackPositionIfFinished(episode)
    player.selectPlayer(episode)
    if (!player.playsLocalFiles || e.downloadInfo.complete)
      stateMachine.transitionToState(new Preparing(e))
    else
      stateMachine.transitionToState(new LocalBuffering(e))
  }

  def joinRemoteSession(episode: EpisodeListItem, state: RemotePlaybackState.RemotePlaybackState): Unit =
    stateMachine.transitionToState(new Preparing(episode, Some(state)))

  private def resetPlaybackPositionIfFinished(episode: EpisodeListItem): EpisodeListItem = {
    if (episode.playbackInfo.finished) {
      AsyncTask.execute(playService.markEpisodeUnfinished(episode.id))
    }
    if (EndPositionChecker.isAtEndPosition(episode)) {
      episode.copy(playbackInfo = episode.playbackInfo.copy(finished = false, playbackPosition = 0))
    } else {
      episode
    }
  }
}
