package mobi.upod.util.concurrent

trait CancelableRunnable extends Runnable {
  private var _cancelled = false

  def cancelled: Boolean = _cancelled

  def cancel(): Unit = {
    _cancelled = true
  }
}
