package mobi.upod.app.gui.chapters

import java.io.File

import android.view.View
import mobi.upod.android.widget.ItemAdapter
import mobi.upod.app.R
import mobi.upod.media.{MediaChapter, MediaChapterTable}

import scala.collection.mutable.ListBuffer

class ChapterListAdapter(chapters: MediaChapterTable, episodeFile: File) extends ItemAdapter[MediaChapter](R.layout.chapter_list_item) {
  private val maxStartTime: Long = chapters.map(_.startMillis).max
  private val maxDuration: Long = chapters.map(_.duration).max
  private val viewHolders = new ListBuffer[ChapterListItemViewHolder]
  private var currentChapter: Option[MediaChapter] = None

  override protected type ItemViewHolder = ChapterListItemViewHolder

  override def items: IndexedSeq[MediaChapter] = chapters.chapters

  override protected def createViewHolder(view: View): ItemViewHolder = {
    val holder = new ChapterListItemViewHolder(view, maxStartTime, maxDuration, episodeFile)
    viewHolders += holder
    holder
  }

  override protected def onBoundView(viewHolder: ChapterListItemViewHolder, position: Int, item: MediaChapter): Unit = {
    super.onBoundView(viewHolder, position, item)
    viewHolder.onChapterChanged(currentChapter)
  }

  override def getItemId(position: Int): Long = chapters(position).startMillis

  def onChapterChanged(chapter: Option[MediaChapter]): Unit = {
    currentChapter = chapter
    viewHolders foreach (_.onChapterChanged(chapter))
  }
}