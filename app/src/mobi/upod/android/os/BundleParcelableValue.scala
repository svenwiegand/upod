package mobi.upod.android.os

import android.os.{Bundle, Parcelable}

class BundleParcelableValue[A <: Parcelable](name: String) extends BundleRefValue[A](name) {

  def put(bundle: Bundle, value: Option[A]) {
    bundle.putParcelable(name, value.getOrElse(null).asInstanceOf[Parcelable])
  }

  def get(bundle: Bundle): Option[A] =
    Option(bundle.getParcelable(name).asInstanceOf[A])
}
