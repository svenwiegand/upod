package mobi.upod.app.gui.episode.library

import android.content.Context
import android.graphics.Point
import android.view.{LayoutInflater, MotionEvent, View}
import com.mobeta.android.dslv.{DragSortController, DragSortListView}
import mobi.upod.android.app.action.ActionState.ActionState
import mobi.upod.android.app.action.{Action, ActionState}
import mobi.upod.android.content.Theme._
import mobi.upod.android.util.CollectionConverters._
import mobi.upod.app.R
import mobi.upod.app.data.EpisodeListItem
import mobi.upod.app.gui.MainNavigation
import mobi.upod.app.gui.episode.{EpisodeListItemAdapter, EpisodeListItemViewHolder, EpisodeListItemViewHolderConfiguration, EpisodeOrderControl}

private[episode] abstract class SortableLibraryEpisodeListFragment(
    protected val navigationItemId: Long,
    protected val emptyTextId: Int)
  extends LibraryEpisodeListFragmentBase {

  override protected val swipeRefreshEnabled: Boolean = false

  private var pinnedEpisodeCount = 0

  protected override def createActions: Map[Int, Action] = Map(
    R.id.action_move_to_top -> MoveToTopAction,
    R.id.action_move_to_bottom -> MoveToBottomAction
  )

  def pinFirstEpisodes(count: Int): Unit =
    pinnedEpisodeCount = count

  def unpinAllEpisodes(): Unit =
    pinnedEpisodeCount = 0

  protected override def createListView(inflater: LayoutInflater) = {
    val listView = inflater.inflate(R.layout.sortable_list_view, null).asInstanceOf[DragSortListView]

    val dragController = new DragController(listView)
    listView.setFloatViewManager(dragController)
    listView.setOnTouchListener(dragController)
    listView.setMaxScrollSpeed(2f)
    listView
  }

  protected def createAdapter(data: IndexedSeq[EpisodeListItem]) = {
    def config = ViewHolderConfiguration(true, false, true, Some(OrderControl))
    new EpisodeListItemAdapter(data, createViewHolder(_, config)) with DragSortListView.DropListener {
      def drop(from: Int, to: Int) {
        move(from, to)
        onEpisodeMoved(from, to, true)
      }
    }
  }

  protected def createViewHolder(view: View, config: EpisodeListItemViewHolderConfiguration): EpisodeListItemViewHolder

  protected def viewModeId = MainNavigation.viewModeIdEpisodes

  protected def onEpisodeMoved(from: Int, to: Int, commit: Boolean): Unit

  protected def invalidateOptionsMenu(): Unit =
    Option(getActivity).foreach(_.invalidateOptionsMenu())

  private class DragController(listView: DragSortListView)
    extends DragSortController(listView, R.id.dragHandle, DragSortController.ON_DOWN, 0) {

    setBackgroundColor(getActivity.getThemeColor(R.attr.windowBackground))

    override def startDragPosition(ev: MotionEvent) = {
      val listPosition = super.startDragPosition(ev)
      if (listPosition < pinnedEpisodeCount)
        DragSortController.MISS
      else
        listPosition
    }

    override def onDragFloatView(floatView: View, floatPoint: Point, touchPoint: Point) {
      if (listView.getFirstVisiblePosition < pinnedEpisodeCount) {
        val lastPinnedIndex = if (pinnedEpisodeCount < listView.getCount) pinnedEpisodeCount - 1 else math.max(listView.getCount - 1, 0)
        Option(listView.getChildAt(lastPinnedIndex)) foreach { lastPinnedView =>
          val minY = lastPinnedView.getBottom
          floatPoint.y = math.max(floatPoint.y, minY)
        }
      }
      super.onDragFloatView(floatView, floatPoint, touchPoint)
    }
  }

  private object OrderControl extends EpisodeOrderControl {

    override protected def pinnedCount: Int = pinnedEpisodeCount

    override protected def size: Int =
      adapter.getCount

    override protected def move(from: Int, to: Int, commit: Boolean): Unit = {
      adapter.asInstanceOf[EpisodeListItemAdapter].move(from, to)
      onEpisodeMoved(from, to, commit)
    }
  }

  private class MoveAction(canMove: Seq[Int] => Boolean, mov: Seq[Int] => Unit) extends Action {

    private def checkedItemPositions: Seq[Int] = {
      val headerCount = getListView.getHeaderViewsCount
      getListView.getCheckedItemPositions.map(_ - headerCount)
    }

    override def state(context: Context): ActionState =
      if (canMove(checkedItemPositions)) ActionState.enabled else ActionState.gone

    override def onFired(context: Context): Unit = {
      mov(checkedItemPositions)
      uncheckAllEpisodes()
    }
  }

  private object MoveToTopAction extends MoveAction(OrderControl.canMoveToTop, OrderControl.moveToTop)
  private object MoveToBottomAction extends MoveAction(OrderControl.canMoveToBottom, OrderControl.moveToBottom)
}
