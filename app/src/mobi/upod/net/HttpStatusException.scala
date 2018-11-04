package mobi.upod.net

import java.io.IOException

class HttpStatusException(val status: Int) extends IOException(s"received HTTP status code $status")

object HttpStatusException {
  def throwIfError(status: Int) {
    if (status >= 300) {
      throw new HttpStatusException(status)
    }
  }
}