package mobi.upod.android.os

import android.os.Bundle

class BundleEnumValue[E <: Enumeration](enum: E)(name: String) extends BundleRefValue[E#Value](name) {

  def put(bundle: Bundle, value: Option[E#Value]): Unit =
    bundle.putString(name, value.map(_.toString).getOrElse(null))

  def get(bundle: Bundle): Option[E#Value] =
    Option(bundle.getString(name)).map(enum.withName)
}
