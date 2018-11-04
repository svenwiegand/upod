package mobi.upod.util

import scala.collection.mutable

trait Observable[A] {
  private var _listeners = Set[A]()
  private val _weakListeners = new mutable.WeakHashMap[A, Unit]()

  protected def listeners = _listeners ++ _weakListeners.synchronized(_weakListeners.keys)

  /** Adds a listener to the observable.
    *
    * @param listener the listener to be added
    * @param fireActiveState whether to immediately inform the listeners about the observable's active state by firing
    *                        the specific events.
    */
  def addListener(listener: A, fireActiveState: Boolean = true): Unit = {
    _listeners = _listeners + listener
    if (fireActiveState) {
      this.fireActiveState(listener)
    }
  }

  def addWeakListener(listener: A, fireActiveState: Boolean = true): Unit = {
    _weakListeners.synchronized(_weakListeners.put(listener, ()))
    if (fireActiveState) {
      this.fireActiveState(listener)
    }
  }

  protected def fireActiveState(listener: A): Unit

  def removeListener(listener: A): Unit = {
    _listeners = _listeners - listener
    _weakListeners.synchronized(_weakListeners.remove(listener))
  }

  protected def fire(event: A => Unit): Unit =
    listeners.foreach(event)
}
