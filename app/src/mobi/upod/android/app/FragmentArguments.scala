package mobi.upod.android.app

import android.app.Fragment
import android.os.Bundle
import mobi.upod.android.os.{BundleValValue, BundleRefValue}

trait FragmentArguments extends Fragment {

  def ensureArgs: Bundle = Option(getArguments) match {
    case Some(args) =>
      args
    case None =>
      val args = new Bundle()
      setArguments(args)
      args
  }

  def getArgument[A <: AnyVal](arg: BundleValValue[A]): A =
    Option(getArguments).map(arg.get(_)).getOrElse(arg.default)

  def getArgument[A <: AnyRef](arg: BundleRefValue[A]): Option[A] =
    Option(getArguments).flatMap(arg.get(_))

  def putArgument[A <: AnyVal](arg: BundleValValue[A], value: A) {
    arg.put(ensureArgs, value)
  }

  def putArgument[A <: AnyRef](arg: BundleRefValue[A], value: Option[A]) {
    arg.put(ensureArgs, value)
  }

  def putArgument[A <: AnyRef](arg: BundleRefValue[A], value: A) {
    arg.put(ensureArgs, Some(value))
  }
}
