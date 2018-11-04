package mobi.upod.android.os

import mobi.upod.util.Observable

trait AsyncObservable[A] extends Observable[A] with CreateThreadDetector {
  private var _synchronousListeners = Set[A]()
  protected val threadExecutor = ThreadExecutor()

  def addSynchronousListener(listener: A, fireActiveState: Boolean = true) {
    _synchronousListeners = _synchronousListeners + listener
    if (fireActiveState) {
      this.fireActiveState(listener)
    }
  }

  def removeSynchronousListener(listener: A) {
    _synchronousListeners = _synchronousListeners - listener
  }

  override protected def fire(event: (A) => Unit) {
    if (isOnCreateThread)
      fireSynchronously(event)
    else
      fireAsynchronously(event)
  }

  protected def fireSynchronously(event: (A) => Unit) {
    _synchronousListeners foreach { event }
    listeners.foreach(event)
  }

  protected def fireAsynchronously(event: (A) => Unit) {
    _synchronousListeners foreach { event }
    threadExecutor post { listeners.foreach(event) }
  }
}
