package mobi.upod.net

import java.io.IOException
import mobi.upod.util.ExceptionUtil

class HttpConnectException(cause: IOException) extends IOException(cause)

object HttpConnectException {
  def throwOnFailure[A](block: A): A =
    ExceptionUtil.map[IOException, A](new HttpConnectException(_))(block)
}