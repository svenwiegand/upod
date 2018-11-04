package mobi.upod.util

object Collections {

  implicit class Index(val index: Int) extends AnyVal {

    def validIndex: Option[Int] = if (index >= 0) Some(index) else None
  }
}
