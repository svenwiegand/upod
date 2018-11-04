package mobi.upod.app.gui.podcast

import mobi.upod.app.data.PodcastListItem

trait PodcastSelectionListener {

  def onPodcastSelected(podcast: PodcastListItem)
}
