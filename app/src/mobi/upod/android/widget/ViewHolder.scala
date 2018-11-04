package mobi.upod.android.widget

trait ViewHolder[A] {
  def setItem(position: Int, item: A)
}
