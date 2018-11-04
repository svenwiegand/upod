package mobi.upod.android.widget

trait GroupViewHolder[A] extends ViewHolder[A] {

  def setGroupPosition(firstInGroup: Boolean) {
    // do nothing by default
  }
}
