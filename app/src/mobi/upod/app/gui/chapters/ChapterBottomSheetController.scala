package mobi.upod.app.gui.chapters

import android.content.Context
import android.view._
import android.widget.ListView
import com.escalatesoft.subcut.inject.{BindingModule, Injectable}
import mobi.upod.android.view.ChildViews
import mobi.upod.android.widget.bottomsheet.BottomSheet
import mobi.upod.app.R
import mobi.upod.app.data.EpisodeListItem
import mobi.upod.app.storage.StoragePreferences
import mobi.upod.media.MediaChapterTable

class ChapterBottomSheetController(
    context: Context,
    val episode: EpisodeListItem,
    chapters: MediaChapterTable,
    bottomSheet: BottomSheet)(
    implicit val bindingModule: BindingModule)
  extends ActionMode.Callback
  with ChildViews
  with Injectable {

  protected val chapterList = childAs[ListView](R.id.chapterList)
  protected val chapterListAdapter = new ChapterListAdapter(chapters, episode.mediaFile(inject[StoragePreferences].storageProvider))
  protected val backgroundColor = episode.extractedOrGeneratedColors.nonLightBackground
  protected val dimmedBackgroundColor = backgroundColor.dimmed

  private var _created = false

  final def create(): ChapterBottomSheetController = {
    if (!_created) {
      _created = true
      onCreate()
    }
    this
  }

  protected def onCreate(): Unit = {
    chapterList.setBackgroundColor(backgroundColor)
    chapterList.setAdapter(chapterListAdapter)

    bottomSheet.setActionModeCallback(this)
    bottomSheet.showPersistent()
  }

  final def destroy(): Unit = {
    onDestroy()
    _created = false
  }

  protected def onDestroy(): Unit = {
    bottomSheet.close()
    bottomSheet.setActionModeCallback(null)
  }

  override def findViewById(id: Int): View = bottomSheet.findViewById(id)

  def toggleBottomSheet(): Unit = bottomSheet.getStatus match {
    case BottomSheet.STATUS_OPEN => bottomSheet.showPersistent()
    case _ => bottomSheet.open()
  }

  //
  // ActionMode.Callback
  //

  override def onCreateActionMode(mode: ActionMode, menu: Menu): Boolean = {

    def findActionModeBar: Option[ViewGroup] = {
      val dummyView = new View(context)
      mode.setCustomView(dummyView)
      val bar = dummyView.getParent match {
        case bar: ViewGroup => Some(bar)
        case _ => None
      }
      mode.setCustomView(null)
      bar
    }

    def colorActionModeBar(): Unit = findActionModeBar foreach { actionModeBar =>
      actionModeBar.setBackgroundColor(dimmedBackgroundColor)
    }

    def setTitle(): Unit = {
      mode.setTitle(R.string.chapters)
      mode.setSubtitle(episode.title)
    }

    colorActionModeBar()
    setTitle()
    true
  }

  override def onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean = false

  override def onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean = false

  override def onDestroyActionMode(mode: ActionMode): Unit = ()
}