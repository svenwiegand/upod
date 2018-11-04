package mobi.upod.app.gui.episode

import mobi.upod.app.data.EpisodeListItem

private[episode] trait EpisodeDetailsHolder {

  def navigationItemId: Long

  def removeEpisodeFromList(episode: EpisodeListItem): Unit
}
