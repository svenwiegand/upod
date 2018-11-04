package mobi.upod.app.services.sync

import java.net.URL

import mobi.upod.app.data.EpisodeReference
import mobi.upod.util.Cursor
import org.joda.time.DateTime

trait Syncer {

  def persistSyncStatus(): Unit

  def commitChanges(): Unit

  def deleteSyncData(): Unit

  def getLastSyncTimestamp: Option[DateTime]

  def getSettings: Option[IdentitySettings]

  def putSettings(settings: IdentitySettings): Unit

  //
  // device
  //

  def getDeviceSyncTimestamp(deviceId: String): Option[DateTime]

  def putDeviceSyncTimestamp(deviceId: String, syncTimestamp: DateTime): Unit

  //
  // subscriptions
  //

  def getSubscriptions: Cursor[Subscription]

  def putSubscriptions(subscriptions: Seq[Subscription]): Unit

  def deleteSubscriptions(subscriptions: Seq[URL]): Unit

  def getEpisodePodcasts(changedAfter: Option[DateTime]): Cursor[URL]

  //
  // episode status
  //

  def getEpisodeStatus(podcast: URL, from: Option[DateTime]): Cursor[EpisodeStatusSyncInfo]

  def putNewEpisodes(episodes: Seq[EpisodeReference], timestamp: DateTime): Unit

  def putLibraryEpisodes(episodes: Seq[EpisodeReference], timestamp: DateTime): Unit

  def putStarredEpisodes(episodes: Seq[EpisodeReference], timestamp: DateTime): Unit

  def putFinishedEpisodes(episodes: Seq[EpisodeReference], timestamp: DateTime): Unit

  def deleteEpisodes(episodes: Seq[EpisodeReference]): Unit

  def deletePodcastEpisodes(podcasts: Seq[URL]): Unit

  def deleteUnreferencedEpisodes(): Unit

  def putPlaybackInfos(playbackInfos: Seq[EpisodePlaybackInfo]): Unit

  //
  // playlist
  //

  def getPlaylist: Cursor[EpisodeReference]

  def putPlaylist(playlist: Seq[EpisodeReference]): Unit
}

