package mobi.upod.net

import java.net.URLEncoder
import mobi.upod.io.CharsetName

object UrlEncoder {

  def encode(s: String) = URLEncoder.encode(s, CharsetName.utf8)
}