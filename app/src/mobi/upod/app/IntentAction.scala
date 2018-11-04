package mobi.upod.app

object IntentAction {
  val actionBase = "mobi.upod.app.intent.action"

  def apply(action: String) = s"$actionBase.$action"
}
