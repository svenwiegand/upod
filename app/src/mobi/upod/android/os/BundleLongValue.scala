package mobi.upod.android.os

import android.os.Bundle

class BundleLongValue(name: String, default: Long = 0) extends BundleValValue(name, default) {

  def put(bundle: Bundle, value: Long) {
    bundle.putLong(name, value)
  }

  def get(bundle: Bundle) = bundle.getLong(name, default)
}
