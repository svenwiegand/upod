package mobi.upod.util

import scala.reflect.ClassTag
import scala.util.Try


object ExceptionUtil {

  def map[A <: Throwable, B](mapException: A => Throwable)(block: B)(implicit classTag: ClassTag[A]): B = {
    try {
      block
    } catch {
      case ex: A =>
        throw mapException(ex)
    }
  }

  def tryAndRecover[A](f: => A)(recover: Throwable => A): A =
    Try(f).recover{ case error: Throwable => recover(error) }.get
}
