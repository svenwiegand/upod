package mobi.upod.app

object IntentExtraKey {
  val extraBase = "mobi.upod.app.intent.data"

  def apply(extra: String) = s"$extraBase.$extra"
}
