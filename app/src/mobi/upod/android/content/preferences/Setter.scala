package mobi.upod.android.content.preferences

trait Setter[A] extends Preference[A] {

  def :=(value: A) {
    write(value)
  }
}
