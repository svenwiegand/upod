package mobi.upod.app.services.download

import mobi.upod.app.data.EpisodeBaseWithDownloadInfo

private[download] trait DownloadController {
  def addListener(listener: DownloadListener, fireActiveState: Boolean = true)

  def removeListener(listener: DownloadListener)

  def downloadQueue(stopOnMeteredConnection: Boolean)

  def downloadEpisode(episode: EpisodeBaseWithDownloadInfo, stopOnMeteredConnection: Boolean)

  def stopAllDownloads()

  def bufferEpisode(episode: EpisodeBaseWithDownloadInfo)

  def stopBuffering()
}
