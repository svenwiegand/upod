package mobi.upod.android.os


abstract class BundleValValue[A <: AnyVal](name: String, val default: A) extends BundleValue[A](name)
