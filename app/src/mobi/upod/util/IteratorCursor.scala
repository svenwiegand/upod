package mobi.upod.util

class IteratorCursor[A](iterator: Iterator[A], doClose: => Unit) extends Cursor[A] {

  override def close(): Unit =
    doClose

  override def next(): A =
    iterator.next()

  override def hasNext: Boolean =
    iterator.hasNext
}
