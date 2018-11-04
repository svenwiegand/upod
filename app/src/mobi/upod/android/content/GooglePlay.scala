package mobi.upod.android.content

object GooglePlay {

  def intentUrl(packageName: String): String =
    s"market://details?id=$packageName"

  def webUrl(packageName: String): String =
    s"http://play.google.com/store/apps/details?id=$packageName"
}
