package mobi.upod.app.services.playback.state

import mobi.upod.app.data.EpisodeListItem

private[playback] trait StateWithEpisode extends PlaybackState {
  def episode: EpisodeListItem
}
