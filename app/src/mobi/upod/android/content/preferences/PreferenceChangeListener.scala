package mobi.upod.android.content.preferences

trait PreferenceChangeListener[A] {

  def onPreferenceChange(newValue: A): Unit
}

object PreferenceChangeListener {

  def apply[A](handle: A => Unit) = new PreferenceChangeListener[A] {
    override def onPreferenceChange(newValue: A): Unit =
      handle(newValue)
  }
}