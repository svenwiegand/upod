package mobi.upod.app.gui.podcast

import android.app.ListFragment
import android.content.Loader
import android.os.Bundle
import android.view.View
import android.widget.{AbsListView, ListView, AdapterView}
import mobi.upod.android.app.{NavigationItemSelection, ListenerFragment}
import mobi.upod.android.content.AsyncCursorLoader
import mobi.upod.android.content.IntentHelpers._
import mobi.upod.android.view.{FragmentViewFinder, ChildViews}
import mobi.upod.app.data.PodcastListItem
import mobi.upod.app.gui.{ReloadOnEpisodeListChangedFragment, PodcastSelection, MainNavigation}
import mobi.upod.app.services.EpisodeService
import mobi.upod.app.services.sync.{SyncListener, SyncService}
import mobi.upod.app.storage.{ImageSize, PodcastDao}
import mobi.upod.app.{R, AppInjection}
import mobi.upod.util.Collections._
import mobi.upod.util.Cursor
import mobi.upod.android.util.CollectionConverters._

class PodcastListFragment
  extends ListFragment
  with ChildViews
  with AppInjection
  with FragmentViewFinder
  with ListenerFragment
  with SyncListener
  with ReloadOnEpisodeListChangedFragment[IndexedSeq[PodcastListItem]]
  with AdapterView.OnItemClickListener {

  private lazy val podcastDao = inject[PodcastDao]

  protected val observables = Traversable(inject[EpisodeService], inject[SyncService])

  private def intentPodcastSelection = getActivity.getIntent.getExtra(PodcastSelection)

  private def adapter = getListAdapter.asInstanceOf[PodcastListItemAdapter]

  private def selectionListener = getActivity.asInstanceOf[PodcastSelectionListener]

  override def onActivityCreated(savedInstanceState: Bundle) {
    super.onActivityCreated(savedInstanceState)

    getListView.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE)
    getListView.setOnItemClickListener(this)
    getListView.setDivider(getResources.getDrawable(R.drawable.coverart_list_divider))
  }

  def removeSelected(): Boolean = {
    val selectedPosition = getListView.getCheckedItemPosition.validIndex
    val selectedItemId = selectedPosition.map(adapter.items(_).id)
    selectedItemId.foreach(id => adapter.remove(Set(id)))

    val newSelectedPosition = selectedPosition match {
      case Some(position) if position >= 0 && position < adapter.getCount =>
        Some(position)
      case Some(position) if position >= adapter.getCount && !adapter.isEmpty =>
        Some(adapter.getCount - 1)
      case _ =>
        None
    }
    newSelectedPosition match {
      case Some(position) =>
        getListView.setItemChecked(position, true)
        getListView.setSelection(position)
        selectionListener.onPodcastSelected(adapter.items(position))
        true
      case None =>
        false
    }
  }

  private def setAdapter(data: IndexedSeq[PodcastListItem]): Unit = Option(adapter) match {
    case Some(a) =>
      a.setItems(data)
    case None =>
    setListAdapter(new PodcastListItemAdapter(data, R.layout.podcast_list_item, ImageSize.list, false))
  }

  def onCreateLoader(id: Int, args: Bundle) = AsyncCursorLoader(getActivity,
    PodcastListFragment.loadPodcasts(getActivity.getIntent.getExtra(NavigationItemSelection), podcastDao))

  def onLoadFinished(loader: Loader[IndexedSeq[PodcastListItem]], data: IndexedSeq[PodcastListItem]) {
    val listView = getListView

    def selectRequestedPodcast(podcast: PodcastListItem): Boolean = {
      val selectedPosition = data.indexWhere(_.id == podcast.id)
      if (selectedPosition >= 0) {
        listView.setItemChecked(selectedPosition, true)
        listView.setSelection(selectedPosition)
        true
      } else {
        false
      }
    }

    def selectPodcastNearestTo(podcast: PodcastListItem): Unit = {
      val podcastTitle = podcast.title.toLowerCase
      val posToSelect = data.indexWhere(_.title.toLowerCase >= podcastTitle).validIndex.getOrElse(data.size - 1)
      if (posToSelect >= 0) {
        listView.setItemChecked(posToSelect, true)
        listView.setSelection(posToSelect)
        selectionListener.onPodcastSelected(data(posToSelect))
      } else {
        clearCheckSelection()
      }
    }

    def clearCheckSelection(): Unit = {
      listView.getCheckedItemPositions.filter(_ < listView.getCount).foreach{ pos =>
        listView.setItemChecked(pos, false)
      }
    }

    clearCheckSelection()
    setAdapter(data)
    if (data.isEmpty) {
      getActivity.finish()
    } else {
      intentPodcastSelection match {
        case Some(podcast) =>
          if (!selectRequestedPodcast(podcast)) {
            selectPodcastNearestTo(podcast)
          }
        case None =>
          clearCheckSelection()
      }
    }
  }

  def onLoaderReset(loader: Loader[IndexedSeq[PodcastListItem]]) {
    setAdapter(IndexedSeq())
    val listView = optionalChildAs[ListView](android.R.id.list)
    if (listView.isDefined && listView.get.isActivated) {
      setListShown(false)
    }
  }

  def onItemClick(parent: AdapterView[_], view: View, position: Int, id: Long) {
    selectionListener.onPodcastSelected(adapter.items(position))
  }
}

private[podcast] object PodcastListFragment {

  def loadPodcasts(navigationItemId: Long, dao: PodcastDao): Cursor[PodcastListItem] = navigationItemId match {
    case MainNavigation.`newEpisodes` => dao.findNewListItems
    case MainNavigation.podcasts => dao.findPodcastListItems
    case MainNavigation.unfinishedEpisodes => dao.findUnfinishedLibraryListItems
    case MainNavigation.audioEpisodes => dao.findUnfinishedAudioLibraryListItems
    case MainNavigation.videoEpisodes => dao.findUnfinishedVideoLibraryListItems
    case MainNavigation.downloadedEpisodes => dao.findUnfinishedDownloadedLibraryListItems
    case MainNavigation.starred => dao.findStarredListItems
    case MainNavigation.finishedEpisodes => dao.findRecentlyFinishedLibraryListItems
  }
}
