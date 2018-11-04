package mobi.upod.android.logging

import com.crashlytics.android.Crashlytics
import org.slf4j.LoggerFactory

class Logger(val cls: Class[_]) extends mobi.upod.logging.Logger {

  private val log = LoggerFactory.getLogger(cls)

  def isTraceEnabled = log.isTraceEnabled

  def isDebugEnabled = log.isDebugEnabled

  def isInfoEnabled = log.isInfoEnabled

  def isWarnEnabled = log.isWarnEnabled

  def isErrorEnabled = log.isErrorEnabled

  def trace(msg: => Any, exception: Throwable): Unit = if (isTraceEnabled) {
    log.trace(msg.toString, exception)
  }

  def debug(msg: => Any, exception: Throwable): Unit = if (isDebugEnabled) {
    log.debug(msg.toString, exception)
  }

  def info(msg: => Any, exception: Throwable): Unit = if (isInfoEnabled) {
    log.info(msg.toString, exception)
  }

  def warn(msg: => Any, exception: Throwable): Unit = if (isWarnEnabled) {
    log.warn(msg.toString, exception)
  }

  def error(msg: => Any, exception: Throwable): Unit = if (isErrorEnabled) {
    log.error(msg.toString, exception)
  }

  def crashLogInfo(msg: => Any, exception: Throwable): Unit = if (isInfoEnabled) {
    val m = msg.toString
    log.info(m, exception)
    crashlyticsLogMsg(m, exception)
  }

  def crashLogWarn(msg: => Any, exception: Throwable): Unit = if (isWarnEnabled) {
    val m = msg.toString
    log.warn(m, exception)
    crashlyticsLogMsg(m, exception)
  }

  def crashLogError(msg: => Any, exception: Throwable): Unit = if (isErrorEnabled) {
    val m = msg.toString
    log.error(m, exception)
    crashlyticsLogMsg(m, exception)
  }

  private def crashlyticsLogMsg(msg: String, exception: Throwable): Unit = {
    val message = exception match {
      case null => msg
      case ex => s"$msg (${ex.getMessage})"
    }
    Crashlytics.log(message)
  }

  override def crashLogSend(exception: Throwable): Unit =
    Crashlytics.logException(exception)

  def measure[A](name: => Any, operation: => A): A = {
    if (isTraceEnabled) {
      val start = System.nanoTime()
      val result = operation
      val nanos = System.nanoTime() - start
      trace(s"$name took $nanos ns")
      result
    } else {
      operation
    }
  }
}
