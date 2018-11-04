package mobi.upod.android.app

abstract class SimpleDialogFragmentObject[A <: java.io.Serializable, B <: SimpleDialogFragment[A]](createFragment: => B) {
  val defaultTag = SimpleDialogFragment.defaultTag

  def apply(data: A): B = {
    val fragment = createFragment
    fragment.prepare(data)
    fragment
  }
}
