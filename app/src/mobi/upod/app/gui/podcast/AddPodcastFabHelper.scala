package mobi.upod.app.gui.podcast

import android.widget.AbsListView
import mobi.upod.android.widget.FloatingActionButton
import mobi.upod.app.R

private[podcast] object AddPodcastFabHelper {
  
  def add(fragment: PodcastGridFragment, scrollListener: Option[AbsListView.OnScrollListener] = None): Unit = {
    FloatingActionButton.addToGrid(
      fragment, 
      new AddPodcastByUrlAction, 
      R.drawable.ic_action_add,
      R.string.add_podcast,
      scrollListener).
      setId(R.id.action_add_podcast)
  }
}
