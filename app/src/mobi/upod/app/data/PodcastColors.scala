package mobi.upod.app.data

import android.graphics.Bitmap
import android.support.v7.graphics.Palette
import mobi.upod.android.graphics.Color
import mobi.upod.app.gui.Theme
import mobi.upod.data._

import scala.collection.JavaConverters._

case class PodcastColors(background: Color, key: Option[Color]) {

  lazy val keyOrBackground: Color = key getOrElse background

  lazy val nonLightBackground: Color = {
    val b = background.notLight
    if (b.isValidMaterialBackground) b else keyOrBackground.notLight
  }

  def accentForNonLightBackground(colorOptions: Color*)(fallback: Color): Color =
    colorOptions.find(nonLightBackground.isComplementaryTo) getOrElse fallback

  def accentForNonLightBackground(implicit theme: Theme): Color = {
    val themeAccentColors = Seq(theme.Colors.SecondaryAccent, theme.Colors.Accent)
    val podcastAccent = key.find(_.isValidMaterialAccent)
    val options = podcastAccent.map(_ +: themeAccentColors).getOrElse(themeAccentColors)
    accentForNonLightBackground(options: _*)(theme.Colors.SecondaryAccent)
  }
}

object PodcastColors extends MappingProvider[PodcastColors] {
  import mobi.upod.data.Mapping._

  val Material = IndexedSeq(
    PodcastColors(0xffdd191d), // red
    PodcastColors(0xffd81b60), // pink
    PodcastColors(0xff8e24aa), // purple
    PodcastColors(0xff5e35b1), // deep purple
    PodcastColors(0xff3949ab), // indigo
    PodcastColors(0xff4e6cef), // blue
    PodcastColors(0xff039be5), // light blue
    PodcastColors(0xff00acc1), // cyan
    PodcastColors(0xff00897b), // teal
    PodcastColors(0xff0a8f08), // green
    PodcastColors(0xff7cb342), // light green
    PodcastColors(0xffc0ca33), // lime
    PodcastColors(0xfffdd835), // yellow
    PodcastColors(0xffffb300), // amber
    PodcastColors(0xfffb8c00), // orange
    PodcastColors(0xfff4511e), // deep orange
    PodcastColors(0xff6d4c41), // brown
    PodcastColors(0xff546e7a)  // blue grey
  )

  def apply(color: Int): PodcastColors =
    apply(Color(color), Some(Color(color)))

  def forAny(a: Any): PodcastColors =
    Material(Math.abs(a.hashCode()) % Material.size)

  def fromImage(image: Bitmap): Option[PodcastColors] = {

    def chooseBackgroundColor(palette: Seq[Palette.Swatch]): Color = {
      val swatchesByUsage = palette.sortBy(_.population).reverse
      swatchesByUsage.find(_.color.isValidMaterialBackground).getOrElse(swatchesByUsage.head).color
    }

    def chooseKeyColor(palette: Palette, paletteItems: Seq[Palette.Swatch]): Option[Color] = {
      Option(palette.getVibrantSwatch).map(_.color) orElse {
        val colorfulPaletteItems = paletteItems.filter(c => c.s >= 0.25f && c.l >= 0.25f && c.l <= 0.75f)
        if (colorfulPaletteItems.nonEmpty)
          Option(colorfulPaletteItems).map(_.maxBy(_.population).color)
        else
          None
      }
    }

    val palette = Palette.from(image).generate
    val paletteItems = palette.getSwatches.asScala
    if (paletteItems.nonEmpty) {
      val backgroundColor = chooseBackgroundColor(paletteItems)
      val keyColor = chooseKeyColor(palette, paletteItems)
      Some(PodcastColors(backgroundColor, keyColor))
    } else {
      None
    }
  }

  override val mapping = map(
    "background" -> int,
    "key" -> optional(int)
  )((b, k) => PodcastColors.apply(Color(b), k.map(Color(_))))(c => Some((c.background.argb, c.key.map(_.argb))))

  val optionalMapping: Mapping[Option[PodcastColors]] = Mapping[Option[PodcastColors]] {
    case data: DataObject => data("background") match {
      case _: DataPrimitive => Some(mapping.read(data))
      case _ => None
    }
    case _ => None
  } {
    (factory, name, element) =>
      element match {
        case Some(e) => mapping.write(factory, name, e)
        case None => factory.create(
          name,
          "background" -> factory.none("background"),
          "key" -> factory.none("key"))
      }
  }


  private implicit class ExtendedPaletteItem(val p: Palette.Swatch) extends AnyVal {
    def population = p.getPopulation
    def rgb = p.getRgb
    def h = p.getHsl()(0)
    def s = p.getHsl()(1)
    def l = p.getHsl()(2)
    def color = Color(p.getRgb)
  }
}