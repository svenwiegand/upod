package mobi.upod.android.os

import android.os.Bundle

object BundleHelpers {

  implicit class RichBundle(val bundle: Bundle) extends AnyVal {

    def get[A](extra: BundleValue[A]): A =
      extra.get(bundle)

    def put[A <: AnyVal](extra: BundleValValue[A], value: A) {
      extra.put(bundle, value)
    }

    def put[A <: AnyRef](extra: BundleRefValue[A], value: Option[A]) {
      extra.put(bundle, value)
    }

    def put[A <: AnyRef](extra: BundleRefValue[A], value: A) {
      extra.put(bundle, Some(value))
    }
  }
}
