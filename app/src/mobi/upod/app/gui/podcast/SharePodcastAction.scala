package mobi.upod.app.gui.podcast

import mobi.upod.android.app.action.ShareAction
import mobi.upod.android.app.action.ShareAction.SharedData
import mobi.upod.app.R
import mobi.upod.app.data.PodcastListItem

final class SharePodcastAction(podcast: => Option[PodcastListItem])
  extends ShareAction(R.string.action_share_podcast, _ => SharePodcastAction.sharedData(podcast))

object SharePodcastAction {

  def sharedData(podcast: Option[PodcastListItem]): Option[SharedData] = podcast.map { p =>
    SharedData(p.url.toString, p.title)
  }
}
