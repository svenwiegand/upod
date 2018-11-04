package mobi.upod.app

import mobi.upod.android.logging.Logging

class UncaughtExceptionHandler(defaultHandler: Option[Thread.UncaughtExceptionHandler])
  extends Thread.UncaughtExceptionHandler
  with Logging {

  def uncaughtException(thread: Thread, ex: Throwable) {
    log.error(s"FATAL: UNCAUGHT EXCEPTION IN THREAD $thread", ex)
    defaultHandler.foreach(_.uncaughtException(thread, ex))
  }
}

object UncaughtExceptionHandler {

  def install(app: App): Unit = {
    val defaultHandler = Option(Thread.getDefaultUncaughtExceptionHandler)
    Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler(defaultHandler))
  }
}
