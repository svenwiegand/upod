package mobi.upod.android.graphics

import android.content.Context
import android.graphics.{Color => AColor}
import android.support.v7.graphics.Palette

class Color(val argb: Int) extends AnyVal with Serializable {

  def rgb: Int =
    argb & 0x00ffffff

  def alpha: Int =
    (argb >> 24) & 0xff

  def red: Int =
    (argb >> 16) & 0xff

  def green: Int =
    (argb >> 8) & 0xff

  def blue: Int =
    argb & 0xff

  def alphaFloat: Float =
    alpha / 255f

  def redFloat: Float =
    red / 255f

  def greenFloat: Float =
    green / 255f

  def blueFloat: Float =
    blue / 255f

  def hsv: (Float, Float, Float) = {
    val hsv = Array[Float](0f, 0f, 0f)
    AColor.colorToHSV(argb, hsv)
    (hsv(0), hsv(1), hsv(2))
  }

  def hsl: (Float, Float, Float) = {
    val r = redFloat
    val g = greenFloat
    val b = blueFloat

    val min = Math.min(r, Math.min(g, b))
    val max = Math.max(r, Math.max(g, b))

    val hue = {
      if (max == min)
        0f
      else if (max == r)
        ((60 * (g - b) / (max - min)) + 360) % 360
      else if (max == g)
        (60 * (b - r) / (max - min)) + 120
      else if (max == b)
        (60 * (r - g) / (max - min)) + 240
      else
        0f
    }

    val lightness = (max + min) / 2

    val saturation = {
      if (max == min)
        0f
      else if (lightness <= .5f)
        (max - min) / (max + min)
      else
        (max - min) / (2 - max - min)
    }

    (hue, saturation, lightness)
  }

  /** The hue in degree from 0 to 360 */
  def hue: Float =
    hsl._1

  /** The satuation from 0 to 1 */
  def saturation: Float =
    hsl._2

  /** The lightness from 0 to 1 */
  def lightness: Float =
    hsl._3

  def luminance: Float =
    Color.luminance(redFloat, greenFloat, blueFloat)

  def isLight: Boolean =
    Color.isLight(luminance)

  def isDark: Boolean =
    Color.isDark(luminance)

  def darken(percent: Int): Color = {
    val (h, s, v) = hsv
    val factor = 1f - percent.toFloat / 100f
    Color.hsv(h, s, factor * v, alpha)
  }

  def dimmed: Color =
    darken(20)

  def notLight: Color = {
    if (!isLight)
      this
    else {
      var r = redFloat
      var g = greenFloat
      var b = blueFloat
      while (Color.isLight(Color.luminance(r, g, b))) {
        r = 0.9f * r
        g = 0.9f * g
        b = 0.9f * b
      }
      Color(r, g, b, alphaFloat)
    }
  }

  def css: String =
    s"rgba($redFloat, $greenFloat, $blueFloat, $alphaFloat)"

  def withAlpha(opacity: Int): Color =
    Color(opacity << 24 | (argb & 0x00ffffff))

  def withAlpha(alpha: Float): Color =
    withAlpha((alpha * 0xff).toInt)

  def withFullOpacity: Color =
    Color(rgb, 0xff)

  /** Calculates distance of this color to the specified other color based on [[http://stackoverflow.com/a/2103422/280384]].
    *
    * @param other the color to calculate the distance to
    * @return the distance of this color to `other`
    */
  def distanceTo(other: Color): Float = {
    val c1 = this
    val c2 = other
    val rmean: Int = (c1.red + c2.red) / 2
    val r: Int = c1.red - c2.red
    val g: Int = c1.green - c2.green
    val b: Int = c1.blue - c2.blue
    math.sqrt((((512 + rmean) * r * r) >> 8) + 4 * g * g + (((767 - rmean) * b * b) >> 8)).toFloat
  }

  /** Calculates the distance of this color to the specified other color regarding its hue.
    *
    * @param other the color to calculate the distance to
    * @return the absolute distance in degree ranging from 0 to 180
    */
  def hueDistanceTo(other: Color): Float =
    Color.hueDistance(hue, other.hue)

  def isComplementaryTo(other: Color): Boolean = {
    val (h1, s1, l1) = hsl
    val (h2, s2, l2) = other.hsl
    val hueDistance = Color.hueDistance(h1, h2) / 360
    val saturationDistance = s2 - s1
    val lightnessDistance = l2 - l1
    val minDistanceFactor = 1f - ((math.abs(saturationDistance) + math.abs(lightnessDistance)) / 2f)
    val minDistance = minDistanceFactor * 0.25f
    hueDistance > minDistance
  }

  def isValidMaterialBackground: Boolean = {
    val l = lightness
    0.2f < l && l < 0.8f
  }

  def isValidMaterialAccent: Boolean = {
    val (_, s, l) = hsl
    s >= 0.45f && 0.38f <= l && l <= 0.62f
  }
}

object Color {

  def apply(argb: Int): Color =
    new Color(argb)

  def apply(rgb: Int, opacity: Int): Color =
    new Color((opacity << 24) | rgb)

  def apply(r: Int, g: Int, b: Int, alpha: Int = 0xff): Color =
    new Color(alpha << 24 | r << 16 | g << 8 | b)

  def apply(r: Float, g: Float, b: Float, alpha: Float): Color =
    apply((255 * r).toInt, (255 * g).toInt, (255 * b).toInt, (255 * alpha).toInt)

  def apply(paletteItem: Palette.Swatch): Color =
    new Color(paletteItem.getRgb)

  def hsv(h: Float, s: Float, v: Float, alpha: Int = 0xff): Color =
    apply(AColor.HSVToColor(alpha, Array(h, s, v)))

  def withId(resId: Int)(implicit context: Context): Color =
    apply(context.getResources.getColor(resId))

  implicit def color2int(color: Color): Int = color.argb

  private def luminance(r: Float, g: Float, b: Float): Float =
    0.299f * r + 0.587f * g + 0.114f * b

  def hueDistance(hue1: Float, hue2: Float): Float = {
    val distance = math.abs(hue2 - hue1)
    val circleDistance = if (distance < 180) distance else math.abs(distance - 360)
    circleDistance.toFloat
  }

  def isLight(luminance: Float): Boolean =
    luminance > 0.65f

  def isDark(luminance: Float): Boolean =
    luminance < 0.35f
}
