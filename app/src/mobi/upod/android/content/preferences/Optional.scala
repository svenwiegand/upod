package mobi.upod.android.content.preferences

trait Optional[A <: AnyRef] extends Preference[A] {

  def option: Option[A]

  def get: A = option.getOrElse(null.asInstanceOf[A])

  override def getIf(condition: Boolean): Option[A] =
    if (condition) option else None

  override protected def isNew(value: A): Boolean =
    !option.exists(_ == value)
}
