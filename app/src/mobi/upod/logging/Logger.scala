package mobi.upod.logging

trait Logger {

  def isTraceEnabled: Boolean

  def isDebugEnabled: Boolean

  def isInfoEnabled: Boolean

  def isWarnEnabled: Boolean

  def isErrorEnabled: Boolean

  def trace(msg: => Any, exception: Throwable = null): Unit

  def debug(msg: => Any, exception: Throwable = null): Unit

  def info(msg: => Any, exception: Throwable = null): Unit

  def warn(msg: => Any, exception: Throwable = null): Unit

  def error(msg: => Any, exception: Throwable = null): Unit

  def crashLogInfo(msg: => Any, exception: Throwable = null): Unit

  def crashLogWarn(msg: => Any, exception: Throwable = null): Unit

  def crashLogError(msg: => Any, exception: Throwable = null): Unit

  def crashLogSend(exception: Throwable = null): Unit
}
