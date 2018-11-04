package mobi.upod.app.storage

import com.escalatesoft.subcut.inject.Injectable
import mobi.upod.sql.Implicits._
import mobi.upod.sql.Sql

private[storage] trait IncludeNewEpisodesInLibraryCondition extends Injectable {
  private val library = sql"${EpisodeDao.episode}.${EpisodeDao.library}"
  private val cached = sql"${EpisodeDao.episode}.${EpisodeDao.cached}"
  private lazy val uiPreferences = inject[UiPreferences]
  private lazy val hideNewInLibraryPreference = uiPreferences.hideNewInLibrary

  protected def includeNewEpisodesCondition: Sql =
    if (hideNewInLibraryPreference) sql"$library<>0" else sql"$cached=0"
}
