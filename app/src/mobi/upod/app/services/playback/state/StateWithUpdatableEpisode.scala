package mobi.upod.app.services.playback.state

import mobi.upod.app.data.EpisodeListItem
import org.joda.time.DateTime
import mobi.upod.app.storage.{EpisodeDao, AsyncTransactionTask}
import mobi.upod.util.Duration._
import mobi.upod.app.services.playback.PlaybackService
import mobi.upod.android.os.AsyncTask

private[state] trait StateWithUpdatableEpisode extends StateWithEpisode {
  private val episodeDao = inject[EpisodeDao]
  private val playService = inject[PlaybackService]
  private val ProgressPersistInterval = 10.seconds
  private var latesProgressPersitTime = 0l
  protected val initialEpisode: EpisodeListItem
  private var _episode: EpisodeListItem = initialEpisode.copy(media = initialEpisode.media.copy(duration = player.getDuration))

  def episode = _episode

  protected def reloadEpisodeFromDatabase(block: => Unit): Unit = {
    AsyncTask.execute[Unit](_episode = episodeDao.findListItemById(_episode.id).getOrElse(_episode))(_ => block)
  }

  protected def updateProgress(forcePersist: Boolean = false): Unit = {
    val playerPosition = player.getCurrentPosition
    log.debug(s"updateProgress to $playerPosition")
    if (playerPosition > 0) {
      _episode = _episode.copy(playbackInfo = _episode.playbackInfo.copy(
        playbackPosition = playerPosition,
        playbackPositionTimestamp = DateTime.now))
      fire(_.onPlaybackPositionChanged(_episode))
      persistProgressIfApplicable(forcePersist)
    }
  }

  private def persistProgressIfApplicable(force: Boolean) {
    val time = System.currentTimeMillis()
    if (force || latesProgressPersitTime + ProgressPersistInterval <= time) {
      latesProgressPersitTime = time
      AsyncTransactionTask.execute {
        episodeDao.updatePlaybackPosition(_episode.id, _episode.playbackInfo.playbackPosition, _episode.media.duration, false)
      }
    }
  }

  protected def markFinished(): Unit = {
    AsyncTask.execute(playService.markEpisodeFinished(_episode.id))
  }
}
