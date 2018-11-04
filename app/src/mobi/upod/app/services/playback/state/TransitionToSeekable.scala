package mobi.upod.app.services.playback.state

import mobi.upod.android.os.AsyncTask
import mobi.upod.app.services.EpisodeService
import mobi.upod.media.MediaChapterTable

private[state] trait TransitionToSeekable extends StateWithEpisode {
  private val futureChapters = inject[EpisodeService].futureChapters(episode)

  protected def transitionTo(state: MediaChapterTable => Seekable): Unit = withChaptersDo { chapters =>
    fire(_.onChaptersChanged(chapters))
    stateMachine.transitionToState(state(chapters))
  }

  private def withChaptersDo(f: MediaChapterTable => Unit): Unit =
    AsyncTask.onResult(futureChapters)(f)
}