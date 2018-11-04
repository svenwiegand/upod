package mobi.upod.android.content.preferences

trait OptionSetter[A <: AnyRef] extends Preference[A] with Optional[A] with Setter[A] {

  def :=(value: Option[A]): Unit =
    write(value.getOrElse(null.asInstanceOf[A]))
}
