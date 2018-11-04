package mobi.upod.android.os

import android.os.Bundle
import java.io.Serializable

class BundleSerializableValue[A <: AnyRef with Serializable](name: String) extends BundleRefValue[A](name) {

  def put(bundle: Bundle, value: Option[A]) {
    bundle.putSerializable(name, value.getOrElse(null).asInstanceOf[Serializable])
  }

  def get(bundle: Bundle): Option[A] =
    Option(bundle.getSerializable(name).asInstanceOf[A])
}
