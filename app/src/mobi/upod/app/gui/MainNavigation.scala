package mobi.upod.app.gui

import android.app.Fragment
import mobi.upod.android.app._
import mobi.upod.app.R
import mobi.upod.app.gui.episode.download.DownloadListFragment
import mobi.upod.app.gui.episode.news.{NewPodcastEpisodeListFragment, NewEpisodeListFragment, GroupedNewEpisodeListFragment}
import mobi.upod.app.gui.episode.library._
import mobi.upod.app.gui.episode.online.OnlinePodcastEpisodeListFragment
import mobi.upod.app.gui.episode.playlist.PlaylistFragment
import mobi.upod.app.gui.podcast._
import mobi.upod.app.gui.preference.{SupportActivityAction, PreferenceAction}

object MainNavigation {
  val discover = 10l
  val newEpisodes = 20l
  val podcasts = 30l
  val findPodcasts = 35l
  val lists = 40l
  val playlist = 50l
  val downloads = 60l
  val library = 70l
  val unfinishedEpisodes = 80l
  val audioEpisodes = 81l
  val videoEpisodes = 82l
  val downloadedEpisodes = 83l
  val starred = 90l
  val finishedEpisodes = 110l
  val actions = 1000l
  val settings = 1010l
  val support = 1020l

  val viewModeIdPodcasts = 10
  val viewModeIdEpisodes = 20
  val viewModeIdGroupedEpisodes = 30
  val viewModeIdPodcastEpisodes = 100

  val viewModeIdCategoryArts = 1001
  val viewModeIdCategoryBusiness = 1002
  val viewModeIdCategoryComedy = 1003
  val viewModeIdCategoryEducation = 1004
  val viewModeIdCategoryGamesAndHobbies = 1005
  val viewModeIdCategoryGovernmentAndOrganizations = 1006
  val viewModeIdCategoryHealth = 1007
  val viewModeIdCategoryKidsAndFamily = 1008
  val viewModeIdCategoryMusic = 1009
  val viewModeIdCategoryNewsAndPolitics = 1010
  val viewModeIdCategoryReligionAndSpirituality = 1011
  val viewModeIdCategoryScienceAndMedicine = 1012
  val viewModeIdCategorySocietyAndCulture = 1013
  val viewModeIdCategorySportsAndRecreation = 1014
  val viewModeIdCategoryTechnology = 1015
  val viewModeIdCategoryTvAndFilm = 1016

  val viewModePodcasts = ViewMode(viewModeIdPodcasts, R.string.view_mode_podcasts)
  val viewModeEpisodes = ViewMode(viewModeIdEpisodes, R.string.view_mode_episodes)
  val viewModeGroupedEpisodes = ViewMode(viewModeIdGroupedEpisodes, R.string.view_mode_grouped_episodes)

  val categoryViewModes = IndexedSeq(
    ViewMode(viewModeIdPodcasts, R.string.category_all),
    ViewMode(viewModeIdCategoryArts, R.string.category_arts),
    ViewMode(viewModeIdCategoryBusiness, R.string.category_business),
    ViewMode(viewModeIdCategoryComedy, R.string.category_comedy),
    ViewMode(viewModeIdCategoryEducation, R.string.category_education),
    ViewMode(viewModeIdCategoryGamesAndHobbies, R.string.category_games_n_hobbies),
    ViewMode(viewModeIdCategoryGovernmentAndOrganizations, R.string.category_government_n_organizations),
    ViewMode(viewModeIdCategoryHealth, R.string.category_health),
    ViewMode(viewModeIdCategoryKidsAndFamily, R.string.category_kids_n_family),
    ViewMode(viewModeIdCategoryMusic, R.string.category_music),
    ViewMode(viewModeIdCategoryNewsAndPolitics, R.string.category_news_n_politics),
    ViewMode(viewModeIdCategoryReligionAndSpirituality, R.string.category_religion_n_spirituality),
    ViewMode(viewModeIdCategoryScienceAndMedicine, R.string.category_science_n_medicine),
    ViewMode(viewModeIdCategorySocietyAndCulture, R.string.category_society_n_culture),
    ViewMode(viewModeIdCategorySportsAndRecreation, R.string.category_sports_n_recreation),
    ViewMode(viewModeIdCategoryTechnology, R.string.category_technology),
    ViewMode(viewModeIdCategoryTvAndFilm, R.string.category_tv_n_film)
  )

  val items = IndexedSeq(
    NavigationItem(newEpisodes, R.string.nav_new, R.drawable.ic_nav_new,
      viewModePodcasts, viewModeEpisodes, viewModeGroupedEpisodes),

    NavigationSectionHeader(library, R.string.nav_library),
    NavigationItem(unfinishedEpisodes, R.string.nav_unfinished, R.drawable.ic_nav_all,
      viewModePodcasts, viewModeEpisodes, viewModeGroupedEpisodes),
    NavigationItem(audioEpisodes, R.string.nav_audio, R.drawable.ic_nav_audio,
      viewModePodcasts, viewModeEpisodes, viewModeGroupedEpisodes),
    NavigationItem(videoEpisodes, R.string.nav_video, R.drawable.ic_nav_video,
      viewModePodcasts, viewModeEpisodes, viewModeGroupedEpisodes),
    NavigationItem(downloadedEpisodes, R.string.nav_downloaded, R.drawable.ic_nav_downloaded,
      viewModePodcasts, viewModeEpisodes, viewModeGroupedEpisodes),
    NavigationItem(starred, R.string.nav_starred, R.drawable.ic_nav_starred,
      viewModePodcasts, viewModeEpisodes, viewModeGroupedEpisodes),
    NavigationItem(finishedEpisodes, R.string.nav_finished, R.drawable.ic_nav_finished,
      viewModePodcasts, viewModeEpisodes, viewModeGroupedEpisodes),

    NavigationSeparator(lists),
    NavigationItem(playlist, R.string.nav_playlist, R.drawable.ic_nav_playlist, viewModeEpisodes),
    NavigationItem(downloads, R.string.nav_downloads, R.drawable.ic_nav_download_list, viewModeEpisodes),

    NavigationSectionHeader(discover, R.string.nav_discover),
    NavigationItem(podcasts, R.string.nav_podcasts, R.drawable.ic_nav_podcasts, viewModePodcasts),
    new NavigationItem(findPodcasts, R.string.nav_find_podcasts, R.drawable.ic_nav_find, categoryViewModes, 0) {
      override val windowTitleId = R.string.nav_find_popular_podcasts
    },

    NavigationSeparator(actions),
    NavigationActionItem(settings, R.string.nav_settings, R.drawable.ic_action_settings, new PreferenceAction),
    NavigationActionItem(support, R.string.nav_support, R.drawable.ic_action_support, new SupportActivityAction)
  )

  lazy val itemsById: Map[Long, NavigationDrawerEntry] =
    items.collect { case item: NavigationDrawerEntry => item.id -> item }.toMap

  def createFragment(navItem: NavigationItem, viewMode: ViewMode): Fragment =
    createFragment(navItem.id, viewMode.id)

  def createFragment(navItemId: Long, viewModeId: Long): Fragment = navItemId match {
    // discover
    case `podcasts` => viewModeId match {
      case `viewModeIdPodcasts` => new AllPodcastsGridFragment
      case `viewModeIdPodcastEpisodes` => new OnlinePodcastEpisodeListFragment
    }
    case `findPodcasts` => viewModeId match {
      case `viewModeIdPodcastEpisodes` => new OnlinePodcastEpisodeListFragment
      case `viewModeIdPodcasts` => new PopularPodcastsGridFragment
      case `viewModeIdCategoryArts` => new PopularArtsPodcastsGridFragment
      case `viewModeIdCategoryBusiness` => new PopularBusinessPodcastsGridFragment
      case `viewModeIdCategoryComedy` => new PopularComedyPodcastsGridFragment
      case `viewModeIdCategoryEducation` => new PopularEducationPodcastsGridFragment
      case `viewModeIdCategoryGamesAndHobbies` => new PopularGamesAndHobbiesPodcastsGridFragment
      case `viewModeIdCategoryGovernmentAndOrganizations` => new PopularGovernmentAndOrganizationsPodcastsGridFragment
      case `viewModeIdCategoryHealth` => new PopularHealthPodcastsGridFragment
      case `viewModeIdCategoryKidsAndFamily` => new PopularKidsAndFamilyPodcastsGridFragment
      case `viewModeIdCategoryMusic` => new PopularMusicPodcastsGridFragment
      case `viewModeIdCategoryNewsAndPolitics` => new PopularNewsAndPoliticsPodcastsGridFragment
      case `viewModeIdCategoryReligionAndSpirituality` => new PopularReligionAndSpiritualityPodcastsGridFragment
      case `viewModeIdCategoryScienceAndMedicine` => new PopularScienceAndMedicinePodcastsGridFragment
      case `viewModeIdCategorySocietyAndCulture` => new PopularSocietyAndCulturePodcastsGridFragment
      case `viewModeIdCategorySportsAndRecreation` => new PopularSportsAndRecreationPodcastsGridFragment
      case `viewModeIdCategoryTechnology` => new PopularTechnologyPodcastsGridFragment
      case `viewModeIdCategoryTvAndFilm` => new PopularTVAndFilmPodcastsGridFragment
    }

    // library
    case `newEpisodes` => viewModeId match {
      case `viewModeIdPodcasts` => new NewPodcastsGridFragment
      case `viewModeIdEpisodes` => new NewEpisodeListFragment
      case `viewModeIdGroupedEpisodes` => new GroupedNewEpisodeListFragment
      case `viewModeIdPodcastEpisodes` => new NewPodcastEpisodeListFragment
    }
    case `unfinishedEpisodes` => viewModeId match {
      case `viewModeIdPodcasts` => new UnfinishedPodcastsGridFragment
      case `viewModeIdEpisodes` => new UnfinishedEpisodeListFragment
      case `viewModeIdGroupedEpisodes` => new UnfinishedGroupedEpisodeListFragment
      case `viewModeIdPodcastEpisodes` => new UnfinishedPodcastEpisodeListFragment
    }
    case `audioEpisodes` => viewModeId match {
      case `viewModeIdPodcasts` => new AudioPodcastsGridFragment
      case `viewModeIdEpisodes` => new AudioEpisodeListFragment
      case `viewModeIdGroupedEpisodes` => new AudioGroupedEpisodeListFragment
      case `viewModeIdPodcastEpisodes` => new AudioPodcastEpisodeListFragment
    }
    case `videoEpisodes` => viewModeId match {
      case `viewModeIdPodcasts` => new VideoPodcastsGridFragment
      case `viewModeIdEpisodes` => new VideoEpisodeListFragment
      case `viewModeIdGroupedEpisodes` => new VideoGroupedEpisodeListFragment
      case `viewModeIdPodcastEpisodes` => new VideoPodcastEpisodeListFragment
    }
    case `downloadedEpisodes` => viewModeId match {
      case `viewModeIdPodcasts` => new DownloadedPodcastsGridFragment
      case `viewModeIdEpisodes` => new DownloadedEpisodeListFragment
      case `viewModeIdGroupedEpisodes` => new DownloadedGroupedEpisodeListFragment
      case `viewModeIdPodcastEpisodes` => new DownloadedPodcastEpisodeListFragment
    }
    case `starred` => viewModeId match {
      case `viewModeIdPodcasts` => new StarredPodcastsGridFragment
      case `viewModeIdEpisodes` => new StarredEpisodeListFragment
      case `viewModeIdGroupedEpisodes` => new StarredGroupedEpisodeListFragment
      case `viewModeIdPodcastEpisodes` => new StarredPodcastEpisodeListFragment
    }
    case `finishedEpisodes` => viewModeId match {
      case `viewModeIdPodcasts` => new FinishedPodcastsGridFragment
      case `viewModeIdEpisodes` => new FinishedEpisodeListFragment
      case `viewModeIdGroupedEpisodes` => new FinishedGroupedEpisodeListFragment
      case `viewModeIdPodcastEpisodes` => new FinishedPodcastEpisodeListFragment
    }

    // lists
    case `playlist` => viewModeId match {
      case `viewModeIdEpisodes` => new PlaylistFragment
    }
    case `downloads` => viewModeId match {
      case `viewModeIdEpisodes` => new DownloadListFragment
    }
  }
}
