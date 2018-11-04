package mobi.upod.net

import com.google.api.client.http.AbstractInputStreamContent
import java.io.InputStream

class RecreatableInputStreamHttpContent(contentType: String, inputStream: => InputStream)
  extends AbstractInputStreamContent(contentType) {

  def getLength = -1

  def retrySupported = true

  def getInputStream = inputStream
}
