package mobi.upod.app.data
import java.net.URL
import com.google.code.rome.android.repackaged.com.sun.syndication.feed.atom.Link
import org.jdom.{Attribute, Element}
import scala.collection.JavaConverters._
import scala.util.Try

object FlattrLink {
  val FlattrRel = "payment"
  val FlattrHost = "flattr.com"

  def fromLinks(links: Seq[Link]): Option[URL] = {
    val link = links.find(link => link.getRel == FlattrRel && link.getHref.toLowerCase.contains(FlattrHost))
    link.flatMap(l => Try(Some(new URL(l.getHref))).getOrElse(None))
  }

  def fromForeignMarkup(foreignMarkup: Seq[Element]): Option[URL] = {

    def attribute(e: Element, name: String): Option[String] =
      e.getAttributes.asInstanceOf[java.util.List[Attribute]].asScala.find(_.getName == name).map(_.getValue)

    def hasAttribute(e: Element, name: String, test: String => Boolean): Boolean =
      attribute(e, name).exists(test)

    val href = foreignMarkup.find { e =>
      e.getName == "link" &&
        hasAttribute(e, "rel", _.toLowerCase == FlattrRel) &&
        hasAttribute(e, "href", _.contains(FlattrHost))
    }.flatMap(attribute(_, "href"))
    href.flatMap(l => Try(Some(new URL(l))).getOrElse(None))
  }

  def apply(links: Seq[Link], foreignMarkup: Seq[Element]): Option[URL] =
    fromLinks(links) orElse fromForeignMarkup(foreignMarkup)

  def apply(links: java.util.List[_], foreignMarkup: Any): Option[URL] = {
    val l = links.asInstanceOf[java.util.List[Link]].asScala
    val m = foreignMarkup.asInstanceOf[java.util.List[Element]].asScala
    FlattrLink(l, m)
  }
}
