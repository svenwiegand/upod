package mobi.upod.android.util

import android.os.Build.VERSION_CODES._

object ApiLevel extends Ordered[Int] {
  val IceCreamSandwich = ICE_CREAM_SANDWICH
  val IceCreamSandwichM1 = ICE_CREAM_SANDWICH_MR1
  val JellyBean = JELLY_BEAN
  val JellyBeanM1 = JELLY_BEAN_MR1
  val JellyBeanM2 = JELLY_BEAN_MR2
  val KitKat = KITKAT
  val Lollipop = LOLLIPOP
  val Marshmallow = M

  val apiLevel = android.os.Build.VERSION.SDK_INT

  def compare(that: Int) = apiLevel - that
}
