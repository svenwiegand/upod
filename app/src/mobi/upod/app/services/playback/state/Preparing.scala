package mobi.upod.app.services.playback.state

import java.io.IOException

import com.escalatesoft.subcut.inject.BindingModule
import mobi.upod.app.data.EpisodeListItem
import mobi.upod.app.services.playback.player.AudioFxAvailability._
import mobi.upod.app.services.playback.player.MediaPlayer
import mobi.upod.app.services.playback.player.MediaPlayer.{OnPreparedListener, OnSeekCompleteListener}
import mobi.upod.app.services.playback.{PlaybackError, RemotePlaybackState}
import mobi.upod.app.storage.StoragePreferences

private[playback] final class Preparing(
    val episode: EpisodeListItem,
    remotePlaybackState: Option[RemotePlaybackState.RemotePlaybackState] = None)(
    implicit stateMachine: StateMachine, bindings: BindingModule)
  extends PlaybackState
  with StateWithEpisode
  with TransitionToSeekable
  with OnPreparedListener
  with OnSeekCompleteListener {

  override protected[state] def onEnterState() {
    super.onEnterState()

    def initiateNewPlayback(): Unit = {
      val storageProvider = inject[StoragePreferences].storageProvider
      try {
        player.load(storageProvider, episode, episode.playbackInfo.playbackPosition.toInt)
      } catch {
        case ex: IOException =>
          stateMachine.onError(ex, PlaybackError(PlaybackError.Unknown))
      }
    }

    def joinRemoteSession(): Unit = remotePlaybackState match {
      case Some(RemotePlaybackState.Paused) => transitionTo(new Paused(episode, _, true))
      case Some(RemotePlaybackState.Playing) => transitionTo(new Playing(episode, _, true))
      case _ => transitionToState(new RemoteBuffering(episode))
    }

    player.selectPlayer(episode)
    player.setOnErrorListener(stateMachine)
    player.setOnPlayerDisconnectedListener(stateMachine)
    player.setOnPlaybackSpeedAdjustmentListener(stateMachine)
    player.setOnRemotePlayerStateChanged(stateMachine)
    player.setOnRemoteEpisodeChangedListener(stateMachine)
    player.setOnPreparedListener(this)

    if (remotePlaybackState.isDefined)
      joinRemoteSession()
    else
      initiateNewPlayback()
  }

  def onPrepared(player: MediaPlayer) {
    player.setOnSeekCompleteListener(this)
    player.seekTo(episode.playbackInfo.playbackPosition.toInt, true)
  }

  override def onSeekComplete(mediaPlayer: MediaPlayer): Unit = {
    player.audioFxAvailability match {
      case NotNow | Available =>
        stateMachine.setAudioEffects(episode)
      case _ =>
    }
    transitionTo(new Playing(episode, _))
  }

  override protected[state] def onExitState() {
    player.setOnSeekCompleteListener(null)
    player.setOnPreparedListener(null)
  }
}
