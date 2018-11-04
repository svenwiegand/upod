package mobi.upod.app.services.playback

import mobi.upod.app.data.EpisodeBaseWithPlaybackInfo

trait PlaybackCommandStateListener extends PlaybackListener {
  private var _preparingPlayback = false
  private var _canPlay = false
  private var _canPausePlayback = false
  private var _canSeek = false
  private var _canStopPlayback = false

  def preparingPlayback = _preparingPlayback
  def hasPlaybackEpisode = _canPlay
  def canPlay = _canPlay
  def canPausePlayback = _canPausePlayback
  def canSeek = _canSeek
  def canStopPlayback = _canStopPlayback

  override def onEpisodeChanged(episode: Option[EpisodeBaseWithPlaybackInfo]) {
    updatePlaybackCommandStates(
      preparing = _preparingPlayback,
      canPlay = episode.isDefined,
      canPause = false,
      canSeek = false,
      canStop = false
    )
  }

  override def onPreparingPlayback(episode: EpisodeBaseWithPlaybackInfo) {
    updatePlaybackCommandStates(
      preparing = true,
      canPlay = false,
      canPause = false,
      canSeek = false,
      canStop = false
    )
  }

  override def onPlaybackStarted(episode: EpisodeBaseWithPlaybackInfo) {
    updatePlaybackCommandStates(
      preparing = false,
      canPlay = false,
      canPause = true,
      canSeek = true,
      canStop = true
    )
  }

  override def onPlaybackPaused(episode: EpisodeBaseWithPlaybackInfo) {
    updatePlaybackCommandStates(
      preparing = false,
      canPlay = true,
      canPause = false,
      canSeek = true,
      canStop = true
    )
  }

  override def onPlaybackStopped() {
    updatePlaybackCommandStates(
      preparing = false,
      canPlay = false,
      canPause = false,
      canSeek = false,
      canStop = false
    )
  }

  private def updatePlaybackCommandStates(
    preparing: Boolean,
    canPlay: Boolean,
    canPause: Boolean,
    canSeek: Boolean,
    canStop: Boolean) {

    def updateState(oldState: Boolean, newState: Boolean, update: Boolean => Unit): Boolean = {
      if (oldState != newState) {
        update(newState)
        true
      } else {
        false
      }
    }

    if (
      updateState(_preparingPlayback, preparing, _preparingPlayback = _) ||
      updateState(_canPlay, canPlay, _canPlay = _) ||
      updateState(_canPausePlayback, canPause, _canPausePlayback = _) ||
      updateState(_canSeek, canSeek, _canSeek = _) ||
      updateState(_canStopPlayback, canStop, _canStopPlayback = _)
    ) {
      onPlaybackCommandStateChanged()
    }
  }

  protected def onPlaybackCommandStateChanged() {
  }
}
