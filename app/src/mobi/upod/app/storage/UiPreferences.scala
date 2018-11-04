package mobi.upod.app.storage

import android.app.Application
import mobi.upod.android.content.preferences._
import mobi.upod.app.R

class UiPreferences(app: Application) extends DefaultPreferences(app, R.xml.pref_ui) {

  lazy val hideNewInLibrary = new BooleanPreference("pref_hide_new_in_library", true) with Setter[Boolean]
  lazy val autoAddToPlaylist = new BooleanPreference("pref_auto_add_to_playlist", false) with Setter[Boolean]
  lazy val skipNew = new BooleanPreference("pref_skip_inbox", false) with Setter[Boolean]
  lazy val notifyNewEpisodes = new BooleanPreference("pref_notify_new_episodes", true)

  lazy val showMediaTypeFilter = new BooleanPreference("pref_filter_media_type", true) with Setter[Boolean]
  lazy val showDownloadedFilter = new BooleanPreference("pref_filter_downloaded", true) with Setter[Boolean]

  lazy val primaryDownloadAction = new EnumerationPreference(PrimaryDownloadAction)("pref_primary_download_action", PrimaryDownloadAction.Download)
  lazy val addAsPrimaryNewAction = new EnumerationPreference(AddAsPrimaryNewAction)("pref_add_as_primary_new_action", AddAsPrimaryNewAction.NotDownloaded) with Setter[AddAsPrimaryNewAction.AddAsPrimaryNewAction]

  def useAddActionForDownloadableNewEpisodes =
    addAsPrimaryNewAction.get == AddAsPrimaryNewAction.NotDownloaded || addAsPrimaryNewAction.get == AddAsPrimaryNewAction.Always

  def useAddActionForDownloadedNewEpisodes =
    addAsPrimaryNewAction.get == AddAsPrimaryNewAction.Always

  private val defaultPodcastGridType = PodcastGridType.withName(app.getString(R.string.defaultPodcastGridType))
  lazy val podcastGridType = new EnumerationPreference(PodcastGridType)("pref_podcast_grid_type", defaultPodcastGridType)
  lazy val showPodcastGridTitle = new BooleanPreference("pref_podcast_grid_show_title", true)

  lazy val sortEpisodesAscending = new BooleanPreference("pref_sort_episodes_ascending") with Setter[Boolean]

  def preferences = Seq(
    hideNewInLibrary,
    autoAddToPlaylist,
    skipNew,
    notifyNewEpisodes,

    showMediaTypeFilter,
    showDownloadedFilter,

    primaryDownloadAction,
    addAsPrimaryNewAction,

    podcastGridType,
    showPodcastGridTitle
  )
}
