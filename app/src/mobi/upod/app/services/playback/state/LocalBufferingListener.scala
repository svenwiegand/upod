package mobi.upod.app.services.playback.state

import mobi.upod.app.data.EpisodeBaseWithDownloadInfo

trait LocalBufferingListener {

  def onBufferingStarted(episode: EpisodeBaseWithDownloadInfo)

  def onBufferingProgress(episode: EpisodeBaseWithDownloadInfo, bufferedMillis: Long, requiredMillis: Long)

  def onBufferingStopped(episode: EpisodeBaseWithDownloadInfo)
}
