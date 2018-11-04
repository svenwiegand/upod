package mobi.upod.app.services.subscription


trait OpmlImportCallback {

  def onImportSucceeded() {}

  def onImportFailed(error: OpmlException) {}
}
