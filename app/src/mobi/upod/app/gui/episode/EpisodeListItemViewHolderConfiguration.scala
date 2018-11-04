package mobi.upod.app.gui.episode

import mobi.upod.app.data.EpisodeListItem
import mobi.upod.android.app.ActivityLifecycle

private[episode] case class EpisodeListItemViewHolderConfiguration(
  backgroundDrawable: Int,
  showPodcastTitle: Boolean,
  singlePodcastList: Boolean,
  enableActions: Boolean,
  enableDragHandle: Boolean,
  orderedAscending: () => Boolean,
  updateEpisode: (EpisodeListItem, Boolean) => Unit,
  reload: () => Unit,
  episodeOrderControl: Option[EpisodeOrderControl],
  dismissController: Option[EpisodeDismissController],
  activityLifecycle: ActivityLifecycle)
