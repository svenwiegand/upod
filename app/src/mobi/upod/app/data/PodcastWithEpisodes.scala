package mobi.upod.app.data

import mobi.upod.app.services.sync.{EpisodeSyncInfo, PodcastSyncInfo}
import mobi.upod.data.{Mapping, MappingProvider}

case class PodcastWithEpisodes(podcast: PodcastSyncInfo, episodes: Seq[EpisodeSyncInfo])

object PodcastWithEpisodes extends MappingProvider[PodcastWithEpisodes] {

  import mobi.upod.data.Mapping._

  val mapping: Mapping[PodcastWithEpisodes] = ObjectMapping[PodcastWithEpisodes] {
    json =>
      val podcast = PodcastSyncInfo.jsonMapping.read(json)
      val episodes = seq(EpisodeSyncInfo.jsonMapping(podcast.uri)).read(json("episodes"))
      PodcastWithEpisodes(podcast, episodes)
  } {
    (factory, name, obj) =>
      throw new NotImplementedError("writing podcasts with episodes to JSON isn't supported")
  }
}