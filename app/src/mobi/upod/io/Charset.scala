package mobi.upod.io

object Charset {
  val utf8 = forName(CharsetName.utf8)

  def forName(name: String) = java.nio.charset.Charset.forName(name)
}
