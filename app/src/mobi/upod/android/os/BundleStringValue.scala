package mobi.upod.android.os

import android.os.Bundle

class BundleStringValue(name: String) extends BundleRefValue[String](name) {

  def put(bundle: Bundle, value: Option[String]) {
    bundle.putString(name, value.getOrElse(null))
  }

  def get(bundle: Bundle) =
    Option(bundle.getString(name, null))
}
