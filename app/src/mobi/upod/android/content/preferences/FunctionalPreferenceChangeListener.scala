package mobi.upod.android.content.preferences

class FunctionalPreferenceChangeListener[A](onChange: A => Unit) extends PreferenceChangeListener[A] {

  override def onPreferenceChange(newValue: A): Unit =
    onChange(newValue)
}

object FunctionalPreferenceChangeListener {

  def apply[A](onChange: A => Unit): FunctionalPreferenceChangeListener[A] =
    new FunctionalPreferenceChangeListener[A](onChange)

  def apply[A](onChange: => Unit): FunctionalPreferenceChangeListener[A] =
    new FunctionalPreferenceChangeListener[A](_ => onChange)
}
