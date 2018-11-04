package mobi.upod.app.gui.chapters

import java.io.File

import android.content.Context
import android.view.View
import mobi.upod.android.app.action.ActionState.ActionState
import mobi.upod.android.app.action.{Action, ActionController, ActionState}
import mobi.upod.android.view.Helpers.RichView
import mobi.upod.android.widget.{ActionButtons, ViewHolder}
import mobi.upod.app.R
import mobi.upod.media.MediaChapter
import mobi.upod.util.Duration.LongDuration

class ChapterListItemViewHolder(view: View, maxChapterStart: Long, maxChapterDuration: Long, episodeFile: File)
  extends ViewHolder[MediaChapter]
  with ActionController
  with ActionButtons {

  private val startTimeView = view.childTextView(R.id.chapterStart)
  private val titleView = view.childTextView(R.id.chapterTitle)
  private val maxDurationView = view.childTextView(R.id.maxChapterDuration)
  private val durationView = view.childTextView(R.id.chapterDuration)
  private val currentIndicator = view.childView(R.id.currentChapterIndicator)
  private var chapter: Option[MediaChapter] = None

  init()

  override protected def createActions: Map[Int, Action] = Map(
    R.id.showChapterImage -> ShowChapterImageAction,
    R.id.openChapterLink -> new OpenChapterLinkAction(chapter.flatMap(_.link))
  )

  private def init(): Unit = {
    initActionButtons(view.childViewGroup(R.id.actionButtons))
  }

  override def setItem(position: Int, item: MediaChapter): Unit = {
    chapter = Some(item)
    startTimeView.setText(item.startMillis.formatFullAligned(maxChapterStart))
    titleView.setText(item.title.getOrElse(""))
    maxDurationView.setText(maxChapterDuration.formatHoursMinutesAndSeconds)
    durationView.setText(item.duration.formatHoursMinutesAndSeconds)
    invalidateActionButtons()
  }

  def onChapterChanged(currentChapter: Option[MediaChapter]): Unit =
    setCurrent(chapter == currentChapter)

  private def setCurrent(current: Boolean): Unit = {
    currentIndicator.makeInvisible(!current)
    startTimeView.makeInvisible(current)
  }

  //
  // actions
  //

  private object ShowChapterImageAction extends Action {

    override def state(context: Context): ActionState =
      if (chapter.exists(_.image.nonEmpty)) ActionState.enabled else ActionState.gone

    override def onFired(context: Context): Unit = chapter foreach { c =>
      ChapterImageActivity.start(context, c, episodeFile)
    }
  }
}