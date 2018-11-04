package mobi.upod.android.app


trait AlertDialogListener {

  def onPositiveAlertButtonClicked(dialogTag: String) {
  }

  def onNeutralAlertButtonClicked(dialogTag: String) {
  }

  def onNegativeAlertButtonClicked(dialogTag: String) {
  }

  def onAlertDialogDismissed(dialogTag: String) {
  }

  def onAlertDialogCancelled(dialogTag: String) {
  }
}
