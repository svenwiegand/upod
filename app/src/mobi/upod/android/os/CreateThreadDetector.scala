package mobi.upod.android.os

trait CreateThreadDetector {
  private val createThread = Thread.currentThread()

  protected def isOnCreateThread: Boolean =
    Thread.currentThread() == createThread
}
