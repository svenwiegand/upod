package mobi.upod.app.services.playback.state

import mobi.upod.app.data.{EpisodeBaseWithDownloadInfo, EpisodeListItem}
import com.escalatesoft.subcut.inject.BindingModule
import mobi.upod.app.services.download.{DownloadService, DownloadListener}
import mobi.upod.util.Duration._

private[state] final class LocalBuffering(val episode: EpisodeListItem)(implicit stateMachine: StateMachine, bindings: BindingModule)
  extends Buffering
  with DownloadListener {

  private val MinBufferMillis = 3.minutes
  private lazy val downloadService = inject[DownloadService]

  override protected[state] def onEnterState() {
    super.onEnterState()
    stateMachine.onBufferingStarted(episode)
    downloadService.addWeakListener(this, false)
    if (!downloadService.downloadingEpisode.exists(_.id == episode.id)) {
      downloadService.buffer(episode)
    }
  }

  override protected[state] def onExitState() {
    downloadService.removeListener(this)
    stateMachine.onBufferingStopped(episode)
    super.onExitState()
  }

  private def play() {
    transitionToState(new Preparing(episode))
  }

  //
  // download listener
  //

  private def transitionIfBufferedEnough(bufferedMillis: Long) {
    val requiredMillis = episode.playbackInfo.playbackPosition + MinBufferMillis
    stateMachine.onBufferingProgress(episode, bufferedMillis, requiredMillis)
    if (bufferedMillis >= requiredMillis) {
      play()
    }
  }

  override def onDownloadProgress(episode: EpisodeBaseWithDownloadInfo, bytesPerSecond: Int, remainingMillis: Option[Long]) {
    episode.estimateDownloadedDuration match {
      case Some(downloadedDuration) =>
        transitionIfBufferedEnough(downloadedDuration)
      case None =>
    }
  }

  override def onDownloadStopped(episode: EpisodeBaseWithDownloadInfo) {
    if (episode.downloadInfo.complete)
      play()
    else
      stop()
  }
}
