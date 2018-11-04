package mobi.upod.app.gui

import android.content.Context
import mobi.upod.android.content.Theme._
import mobi.upod.android.graphics.Color
import mobi.upod.app.R

class Theme(context: Context) {
  private implicit val ctx: Context = context

  val isLight = true

  object Colors {
    lazy val Primary = Color withId R.color.primary
    lazy val PrimaryDark = Color withId R.color.primary_dark
    lazy val Accent = Color withId R.color.accent
    lazy val AccentDark = Color withId R.color.accent_dark
    lazy val SecondaryAccent = Color withId R.color.secondary_accent
    lazy val SecondaryAccentDark = Color withId R.color.secondary_accent_dark
    lazy val White = Color withId R.color.white
    lazy val Black = Color withId R.color.black

    lazy val PrimaryTextColor: Color =
      context.getThemeColor(R.attr.textColorPrimary)

    lazy val SecondaryTextColor: Color =
      context.getThemeColor(R.attr.textColorSecondary)

    lazy val PrimaryInverseTextColor: Color =
      context.getThemeColor(R.attr.textColorPrimaryInverse)

    lazy val SecondaryInverseTextColor: Color =
      context.getThemeColor(R.attr.textColorSecondaryInverse)

    lazy val PrimaryLightTextColor: Color =
      Color withId R.color.text_primary_light

    lazy val SecondaryLightTextColor: Color =
      Color withId R.color.text_secondary_light

    lazy val PrimaryDarkTextColor: Color =
      Color withId R.color.text_primary_dark

    lazy val SecondaryDarkTextColor: Color =
      Color withId R.color.text_secondary_dark

    def primaryTextColorForBackground(color: Color): Color =
      if (color.isLight) PrimaryLightTextColor else PrimaryDarkTextColor

    def secondaryTextColorForBackground(color: Color): Color =
      if (color.isLight) SecondaryLightTextColor else SecondaryDarkTextColor
    
    def accentFor(color: Color): Color =
      if (isRed(color)) Accent else SecondaryAccent

    def accentDarkFor(color: Color): Color =
      if (isRed(color)) AccentDark else SecondaryAccentDark

    private def isRed(color: Color): Boolean =
      color.red > color.blue && color.red > color.green
  }

  object Dimensions {
    lazy val ActionBarSize = context.getThemeDimension(R.attr.actionBarSize)
  }
}
