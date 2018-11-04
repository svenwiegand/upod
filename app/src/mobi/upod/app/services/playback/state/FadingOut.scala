package mobi.upod.app.services.playback.state

import java.util.Timer

import com.escalatesoft.subcut.inject.BindingModule
import mobi.upod.app.data.EpisodeListItem
import mobi.upod.media.MediaChapterTable
import mobi.upod.util.TimerTask

import scala.util.Try

private[playback] final class FadingOut(
    val episode: EpisodeListItem,
    val chapters: MediaChapterTable)(
    implicit
    stateMachine: StateMachine,
    bindings: BindingModule)
  extends PlaybackState
  with StateWithEpisode
  with Stoppable {

  private val FadeInterval = 200
  private val FadeIncrement: Float = 1f / (FadingOut.FadeTime / FadeInterval)
  private val fadeTimer = new Timer("FadeOutTimer")
  private val preFadePosition = player.getCurrentPosition
  private var volume = 1f

  override protected[state] def onEnterState(): Unit = {
    super.onEnterState()
    fadeTimer.schedule(TimerTask(fade()), 0, FadeInterval)
  }

  override protected[state] def onExitState(): Unit = {
    fadeTimer.cancel()
    Try(player.setRelativeVolume(1f))
    super.onExitState()
  }

  private def fade(): Unit = {
    volume = math.max(0, volume - FadeIncrement)
    player.setRelativeVolume(volume)
    if (volume == 0) {
      fadeTimer.cancel()
      player.pause()
      player.setRelativeVolume(1f)
      Try(player.seekTo(preFadePosition, true))
      transitionToState(new Paused(episode, chapters, false))
    }
  }
}

private[playback] object FadingOut {
  val FadeTime = 3000
}