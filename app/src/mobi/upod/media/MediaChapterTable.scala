package mobi.upod.media

import scala.collection.GenTraversableOnce

class MediaChapterTable(val chapters: IndexedSeq[MediaChapter]) {
  import MediaChapterTable._

  lazy val reversedChapters = chapters.reverse

  def isEmpty: Boolean = chapters.isEmpty

  def size: Int = chapters.size

  def apply(index: Int): MediaChapter = chapters(index)

  def find(p: MediaChapter => Boolean): Option[MediaChapter] = chapters.find(p)

  def reverseFind(p: MediaChapter => Boolean): Option[MediaChapter] = reversedChapters.find(p)

  def map[B](f: MediaChapter => B): IndexedSeq[B] = chapters.map(f)

  def flatMap[B](f: MediaChapter => GenTraversableOnce[B]): IndexedSeq[B] = chapters.flatMap(f)

  def chapterIndexAt(pos: Long): Option[Int] = {
    withProgressive(pos)(p => chapters.indexWhere(c => c.startMillis <= p && p < c.endMillis)) match {
      case -1 => None
      case n => Some(n)
    }
  }

  def chapterAt(pos: Long): Option[MediaChapter] =
    withProgressive(pos)(p => find(c => c.startMillis <= p && p < c.endMillis))

  def hasNextChapter(pos: Long): Boolean =
    nextChapter(pos).isDefined

  def nextChapter(pos: Long): Option[MediaChapter] =
    withProgressive(pos)(p => find(_.startMillis > p))

  def hasPrevChapter(pos: Long): Boolean =
    prevChapter(pos).isDefined

  def prevChapter(pos: Long): Option[MediaChapter] =
    withProgressive(pos)(p => reverseFind(_.endMillis < p))

  def hasBackChapter(pos: Long): Boolean =
    chapterBack(pos).isDefined

  def chapterBack(pos: Long): Option[MediaChapter] =
    withProgressive(pos)(p => reverseFind(_.startMillis < pos - MediaChapterTable.ChapterStartTolerance))
}

object MediaChapterTable {
  val ChapterStartTolerance = 3000
  private val ChapterPositionTolerance = 1000

  def apply(chapters: IndexedSeq[MediaChapter]): MediaChapterTable = new MediaChapterTable(chapters)

  def apply(): MediaChapterTable = new MediaChapterTable(IndexedSeq())

  /** Provides a slightly forwarded playback position for the provided position.
    *
    * Background: Media player does not seek exactly, but to a position a little bit in front of the requested position.
    * So the chapter query methods work with a little tolerance when determining whether they already reached the next
    * chapter.
    *
    * @param pos the position to progressively forward
    * @return a slightly forwarded position
    */
  private def progressive(pos: Long): Long = pos + ChapterPositionTolerance

  /** Convenience method to `progressive` if required multiple times within a one liner. */
  private def withProgressive[A](pos: Long)(f: Long => A): A = f(progressive(pos))
}