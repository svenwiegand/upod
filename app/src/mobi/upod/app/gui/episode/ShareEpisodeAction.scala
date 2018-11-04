package mobi.upod.app.gui.episode

import mobi.upod.android.app.action.ShareAction
import mobi.upod.android.app.action.ShareAction.SharedData
import mobi.upod.app.R
import mobi.upod.app.data.EpisodeBase

final class ShareEpisodeAction(episode: => Option[EpisodeBase])
  extends ShareAction(R.string.action_share_episode, _ => ShareEpisodeAction.sharedData(episode))

object ShareEpisodeAction {

  def sharedData(episode: Option[EpisodeBase]): Option[SharedData] = episode.map { e =>
    SharedData(e.link.getOrElse(e.media.url.toString), e.title)
  }
}
