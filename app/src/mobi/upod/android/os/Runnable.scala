package mobi.upod.android.os

object Runnable {

  def apply(execute: => Unit) = new Runnable {
    def run() {
      execute
    }
  }
}
