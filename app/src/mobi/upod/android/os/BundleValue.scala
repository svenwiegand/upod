package mobi.upod.android.os

import android.os.Bundle

abstract class BundleValue[A](val name: String) {

  def put(bundle: Bundle, value: A)

  def get(bundle: Bundle): A
}
