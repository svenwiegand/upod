package mobi.upod.app.services.download

import mobi.upod.app.data.EpisodeBaseWithDownloadInfo

trait DownloadListener {

  def onDownloadListChanged(): Unit = {}

  def onDownloadStarted(episode: EpisodeBaseWithDownloadInfo): Unit = {}

  def onDownloadProgress(episode: EpisodeBaseWithDownloadInfo, bytesPerSecond: Int, remainingMillis: Option[Long]): Unit = {}

  def onDownloadStopped(episode: EpisodeBaseWithDownloadInfo): Unit = {}

  def onDownloadsFinished(): Unit = {}

  def onDownloadsCancelled(error: Throwable): Unit = {}
}
