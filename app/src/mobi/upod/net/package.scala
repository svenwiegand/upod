package mobi.upod

import java.io.{FileOutputStream, File}
import java.net.{URI, URL}
import mobi.upod.io._
import org.joda.time.DateTime

package object net {

  implicit class RichUrl(val url: URL) extends AnyVal {

    def downloadTo(file: File): Unit = {
      val connection = new HttpURLConnector(url).connect()
      try copy(connection.getInputStream, new FileOutputStream(file)) finally connection.disconnect()
    }

    def fileExtension: Option[String] = url.toString.fileExtension
  }

  implicit class RichUri(val uri: URI) extends AnyVal {

    def encode = {
      uri.toString.urlEncoded
    }
  }

  implicit class UrlEncodableString(val string: String) extends AnyVal {

    def urlEncoded: String = UrlEncoder.encode(string)
  }

  implicit class UrlStringInterpolator(val urlString: StringContext) extends AnyVal {

    def url(arguments: Any*): PartialUrl = {

      def valueToUrlString(value: Any): String = value match {
        case symbol: Symbol => symbol.name
        case num @ (_: Int | _: Long | _: Double) => num.toString
        case timestamp: DateTime => timestamp.getMillis.toString
        case obj => s"${obj.toString.urlEncoded}"
      }

      val parts = urlString.parts.iterator
      val args = arguments.iterator
      val buf = new StringBuilder(parts.next())
      while (parts.hasNext) {
        buf ++= valueToUrlString(args.next()) ++= parts.next()
      }
      new PartialUrl(buf.mkString)
    }
  }

  class PartialUrl(val url: String) extends AnyVal {

    def withQueryParameters(parameters: (String, Any)*): PartialUrl = {
      val definedParameters = parameters.collect {
        case (name, Some(value)) => name -> value
        case (name, null) => null
        case (name, None) => null
        case x => x
      } filter(_ != null)
      val params = if (definedParameters.nonEmpty)
        new PartialUrl(definedParameters.map(p => url"${p._1}=${p._2}".url).mkString("?", "&", ""))
      else
        ""
      new PartialUrl(url + params)
    }

    override def toString: String = url
  }

  implicit def partialUrl2String(url: PartialUrl): String =
    url.url
}
