package mobi.upod.android.os

import android.os.Bundle

class BundleIntValue(name: String, default: Int = 0) extends BundleValValue(name, default) {

  def put(bundle: Bundle, value: Int) {
    bundle.putInt(name, value)
  }

  def get(bundle: Bundle) = bundle.getInt(name, default)
}
