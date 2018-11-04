package mobi.upod.app.gui.episode

import scala.annotation.tailrec

trait EpisodeOrderControl {

  protected def move(from: Int, to: Int, commit: Boolean): Unit

  protected def size: Int

  protected def pinnedCount: Int

  def canMoveToTop(position: Int): Boolean =
    position > pinnedCount

  def canMoveToBottom(position: Int): Boolean =
    position >= pinnedCount && position < (size - 1)

  def moveToTop(position: Int): Unit = if (canMoveToTop(position)) {
    move(position, pinnedCount, true)
  }

  def moveToBottom(position: Int): Unit = if (canMoveToBottom(position)) {
    move(position, size - 1, true)
  }

  def canMoveToTop(positions: Seq[Int]): Boolean =
    positions.exists(canMoveToTop)

  def canMoveToBottom(positions: Seq[Int]): Boolean =
    positions.exists(canMoveToBottom)

  def moveToTop(positions: Seq[Int]): Unit = {

    @tailrec
    def mv(pos: Seq[Int], offset: Int): Unit = if (pos.nonEmpty) {
      move(pos.head, pinnedCount + offset, pos.size == 1)
      mv(pos.tail, offset + 1)
    }

    val filteredAndSorted = positions.filter(canMoveToTop).sorted
    mv(filteredAndSorted, 0)
  }

  def moveToBottom(positions: Seq[Int]): Unit = {

    @tailrec
    def mv(pos: Seq[Int], offset: Int): Unit = if (pos.nonEmpty) {
      move(pos.head, size - 1 - offset, pos.size == 1)
      mv(pos.tail, offset + 1)
    }

    val filteredAndSorted = positions.filter(canMoveToBottom).sorted.reverse
    mv(filteredAndSorted, 0)
  }
}
