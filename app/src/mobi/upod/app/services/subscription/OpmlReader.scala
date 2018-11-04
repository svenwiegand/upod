package mobi.upod.app.services.subscription

import java.io.{InputStream, PushbackInputStream}
import java.net.URL
import javax.xml.parsers.SAXParserFactory

import mobi.upod.android.logging.Logging
import org.xml.sax
import org.xml.sax._

import scala.util.Try

object OpmlReader extends Logging {

  def read(opml: InputStream): Seq[URL] = {
    val input = new PushbackInputStream(opml)

    def fixProlog(): Unit = {
      var byte = 0
      do {
        byte = input.read()
      } while (byte >= 0 && byte != '<')
      if (byte == '<')
        input.unread(byte)
    }

    fixProlog()
    readOmpl(input)
  }

  private def readOmpl(opml: InputStream): Seq[URL] = {
    val parserFactory = SAXParserFactory.newInstance
    val parser = parserFactory.newSAXParser
    val xmlReader = parser.getXMLReader
    val source = new sax.InputSource(opml)

    val opmlHandler = new OpmlContentHandler
    xmlReader.setErrorHandler(OpmlErrorHandler)
    xmlReader.setContentHandler(opmlHandler)

    xmlReader.parse(source)

    val urls = opmlHandler.urls
    if (urls.isEmpty)
      throw new IllegalArgumentException(s"Could not find any urls")

    urls
  }

  private class OpmlContentHandler extends ContentHandler {
    private val _urls = collection.mutable.Buffer[URL]()
    def urls = _urls.seq
    
    override def startElement(uri: String, localName: String, qName: String, atts: Attributes): Unit = localName match {
      case "outline" =>
        outlineUrl(atts).foreach(_urls += _)
      case _ =>
    }

    private def outlineUrl(attrs: Attributes): Option[URL] = {
      val attributes = for (i <- 0 until attrs.getLength)
        yield attrs.getLocalName(i) -> attrs.getValue(i)

      attributes.find(_._1 == "xmlUrl").flatMap { case (_, url) =>
        Try(Some(new URL(url))).getOrElse(None)
      }
    }

    // not interested
    override def skippedEntity(name: String): Unit = {}
    override def processingInstruction(target: String, data: String): Unit = {}
    override def ignorableWhitespace(ch: Array[Char], start: Int, length: Int): Unit = {}
    override def characters(ch: Array[Char], start: Int, length: Int): Unit = {}
    override def endElement(uri: String, localName: String, qName: String): Unit = {}
    override def endPrefixMapping(prefix: String): Unit = {}
    override def startPrefixMapping(prefix: String, uri: String): Unit = {}
    override def endDocument(): Unit = {}
    override def startDocument(): Unit = {}
    override def setDocumentLocator(locator: Locator): Unit = {}
  }

  private object OpmlErrorHandler extends ErrorHandler {

    override def fatalError(exception: SAXParseException): Unit = {
      throw exception
    }

    override def error(exception: SAXParseException): Unit = {
      log.info(s"ignoring OPML parse error ${exception.getMessage}")
    }

    override def warning(exception: SAXParseException): Unit = {
      log.info(s"ignoring OPML parse warning ${exception.getMessage}")
    }
  }
}
