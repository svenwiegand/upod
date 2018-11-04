package mobi.upod.util

import mobi.upod.io.Charset
import com.google.android.vending.licensing.util.Base64

object Hash {
  val digest = java.security.MessageDigest.getInstance("MD5")

  def apply(str: String): String = {
    val hash = digest.digest(str.getBytes(Charset.utf8))
    Base64.encode(hash)
  }
}
