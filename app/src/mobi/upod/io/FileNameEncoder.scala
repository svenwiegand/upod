package mobi.upod.io

import java.net.URLEncoder

object FileNameEncoder {
  private val InvalidChars = "?*"

  def encode(s: String): String = {
    def encodeChar(s: String, c: Char): String =
      s.replace(c.toString, f"%%$c%02x")

    val baseString = URLEncoder.encode(s, CharsetName.utf8)
    InvalidChars.foldLeft(baseString)(encodeChar)
  }

}
