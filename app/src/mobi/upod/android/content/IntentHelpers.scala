package mobi.upod.android.content

import android.content.Intent
import android.os.Bundle
import mobi.upod.android.os.{BundleValue, BundleValValue, BundleRefValue}

object IntentHelpers {

  implicit class RichIntent(val intent: Intent) extends AnyVal {

    def getExtra[A <: AnyVal](extra: BundleValValue[A]): A =
      Option(intent.getExtras).map(extra.get(_)).getOrElse(extra.default)

    def getExtra[A <: AnyRef](extra: BundleRefValue[A]): Option[A] =
      Option(intent.getExtras).flatMap(extra.get(_))

    def putExtra[A <: AnyVal](extra: BundleValValue[A], value: A) {
      val extras = new Bundle
      extra.put(extras, value)
      intent.putExtras(extras)
    }

    def putExtra[A <: AnyRef](extra: BundleRefValue[A], value: Option[A]) {
      val extras = new Bundle
      extra.put(extras, value)
      intent.putExtras(extras)
    }

    def putExtra[A <: AnyRef](extra: BundleRefValue[A], value: A) {
      val extras = new Bundle
      extra.put(extras, Some(value))
      intent.putExtras(extras)
    }

    def removeExtra(extra: BundleValue[_]) {
      intent.removeExtra(extra.name)
    }
  }
}
