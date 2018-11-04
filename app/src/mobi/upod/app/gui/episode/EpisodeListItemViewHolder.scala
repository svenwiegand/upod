package mobi.upod.app.gui.episode

import android.content.Context
import android.view.{MenuItem, View}
import android.widget.{ImageView, ListView}
import com.escalatesoft.subcut.inject.BindingModule
import mobi.upod.android.view.Helpers._
import mobi.upod.android.view.Tintable
import mobi.upod.android.widget.{GroupViewHolder, ItemContextMenu}
import mobi.upod.app.R
import mobi.upod.app.data.EpisodeListItem

import scala.util.Try

private[episode] abstract class EpisodeListItemViewHolder
  (protected val view: View, protected val config: EpisodeListItemViewHolderConfiguration)
  (implicit val bindingModule: BindingModule)
  extends GroupViewHolder[EpisodeListItem]
  with EpisodeListItemView {

  private lazy val listView = findListView()
  private val divider = view.childAs[View](R.id.divider)
  private val dragHandle = view.childView(R.id.dragHandle)
  private val contextMenu = view.childAs[ItemContextMenu](R.id.contextMenu)

  implicit def context: Context = view.getContext

  createListItemView()
  initView()
  initContextMenu()
  config.activityLifecycle.addWeakListener(this, false)

  protected trait DismissActionInfo {
    val dismissController: Option[EpisodeDismissController] = config.dismissController
    def listView: ListView = EpisodeListItemViewHolder.this.listView
  }

  protected trait EpisodeAdapterUpdate extends EpisodeUpdate {
    protected def updateEpisode(episode: EpisodeListItem) {
      config.updateEpisode(episode, true)
    }
  }

  protected trait FullListReload extends EpisodeUpdate {
    protected def updateEpisode(episode: EpisodeListItem) {
      Try(config.reload())
    }
  }

  def initView() {
    view.setBackgroundResource(config.backgroundDrawable)
  }

  override protected def tintViews(item: EpisodeListItem): Unit = {
    super.tintViews(item)
    mediaProgressView.setTint(if (config.singlePodcastList) itemKeyColor else theme.Colors.Primary)
  }

  def initContextMenu() {
    contextMenu.listener = this
    contextMenu.primaryActionChooser = this
    contextMenu.show(config.enableActions)
  }

  protected def invalidateMenu(): Unit = {
    try {
      if (config.enableActions) {
        contextMenu.invalidateMenu()
      }
      dragHandle.show(shouldShowDragHandle)
    } catch {
      case ex: IllegalArgumentException =>
        // window now longer attached -- don't care
    }
  }

  protected def shouldShowDragHandle: Boolean = config.enableDragHandle

  protected def showPodcastTitle: Boolean = config.showPodcastTitle

  def setItem(position: Int, item: EpisodeListItem): Unit = {
    setEpisode(item)
    updateImageAction(position, item)
    setGroupPosition(position == 0)
  }

  override def setGroupPosition(firstInGroup: Boolean) {
    divider.show(!firstInGroup)
  }

  private def updateImageAction(position: Int, item: EpisodeListItem) {

    def toggleItemChecked() {
      val itemPosition = position + listView.getHeaderViewsCount
      listView.setItemChecked(itemPosition, !listView.isItemChecked(itemPosition))
    }

    if (config.enableActions) {
      imageView.onClick(toggleItemChecked())
    }
  }

  private def findListView(start: View = view): ListView = {
    start.getParent match {
      case listView: ListView => listView
      case other: View => findListView(other)
    }
  }

  //
  // listeners
  //

  override protected def onEpisodeDownloadInfoUpdate(episode: EpisodeListItem, stateUpdate: Boolean): Unit = {
    config.updateEpisode(episode, stateUpdate)
  }

  override protected def onEpisodePlaybackInfoUpdate(episode: EpisodeListItem, stateUpdate: Boolean): Unit = {
    config.updateEpisode(episode, stateUpdate)
  }

  //
  // primary action chooser
  //

  override def onPreparePrimaryAction(item: MenuItem, primaryActionButton: ImageView): Unit = {
    if (item.getItemId == R.id.action_pause)
      Tintable.tint(primaryActionButton, itemKeyColor)
    else
      primaryActionButton.setColorFilter(null)
  }

  //
  // actions
  //

  protected class MarkEpisodesFinishedFromHereAction
    extends MarkThisAndOlderEpisodesFinishedAction(episodeListItem)
    with FullListReload {

    override protected def enabled(episode: EpisodeListItem): Boolean =
      !config.orderedAscending() && super.enabled(episode)
  }

  protected class MarkEpisodesFinishedToHereAction
    extends MarkThisAndOlderEpisodesFinishedAction(episodeListItem)
    with FullListReload {

    override protected def enabled(episode: EpisodeListItem): Boolean =
      config.orderedAscending() && super.enabled(episode)
  }
}
