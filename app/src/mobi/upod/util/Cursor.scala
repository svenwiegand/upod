package mobi.upod.util

import java.io.Closeable

import scala.collection.mutable

trait Cursor[A] extends Iterator[A] with Closeable {
  import mobi.upod.io._

  def doAndClose[B](block: Cursor[A] => B): B =
    forCloseable(this)(block)

  def toListAndClose(): List[A] = doAndClose(_.toList)

  def toSeqAndClose(): Seq[A] = toListAndClose()

  def toIndexedSeqAndClose(): IndexedSeq[A] = doAndClose(_.toIndexedSeq)

  def toSetAndClose(): Set[A] = doAndClose(_.toSet)

  def toBufferAndClose(): mutable.Buffer[A] = doAndClose(_.toBuffer)

  def nextAndClose(): Option[A] = doAndClose { cursor =>
    if (cursor.hasNext) Some(next()) else None
  }

  def foreachAndClose(block: A => Unit): Unit =
    doAndClose(_.foreach(block))

  override def map[B](f: A => B): Cursor[B] =
    new IteratorCursor(super.map(f), close())

  override def zipWithIndex: Cursor[(A, Int)] =
    new IteratorCursor(super.zipWithIndex, close())
}

object Cursor {

  def apply[A](items: Iterable[A]): Cursor[A] = new Cursor[A] {
    private val iterator = items.iterator

    override def hasNext: Boolean =
      iterator.hasNext

    override def next(): A =
      iterator.next()

    override def close(): Unit =
      ()// nothing to do
  }

  def empty[A] = apply[A](Seq())
}
