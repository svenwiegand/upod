package mobi.upod.android.app

import mobi.upod.util.Observable

trait MultiWeakListener {

  protected def observables: Traversable[Observable[_ >: this.type]]

  protected def registerListener() {
    observables.foreach(_.addWeakListener(this))
  }

  protected def unregisterListener() {
    observables.foreach(_.removeListener(this))
  }
}
