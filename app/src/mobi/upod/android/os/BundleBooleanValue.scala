package mobi.upod.android.os

import android.os.Bundle

class BundleBooleanValue(name: String, default: Boolean = false) extends BundleValValue(name, default) {

  def put(bundle: Bundle, value: Boolean) {
    bundle.putBoolean(name, value)
  }

  def get(bundle: Bundle) = bundle.getBoolean(name, default)
}
