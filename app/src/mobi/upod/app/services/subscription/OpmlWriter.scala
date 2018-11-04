package mobi.upod.app.services.subscription

import java.io.{Writer, FileWriter, BufferedWriter, File}
import java.net.URL

import mobi.upod.io.forCloseable
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

import scala.xml.Text

object OpmlWriter {

  def write(file: File, subscriptions: TraversableOnce[(String, URL)]): Unit = {

    def writePreamble()(implicit writer: Writer): Unit = {
        writer.write(
          s"""<?xml version="1.0" encoding="UTF-8"?>
             |<opml version="2.0">
             |""".stripMargin)
    }

    def writeHead()(implicit writer: Writer): Unit = {
      val timestamp = DateTime.now
      val formattedTimestamp = DateTimeFormat.forPattern("YYYY-MM-dd HH:mm:ss").print(timestamp)
      writer.write(
        s"""<head>
           |  <title>uPod Subscriptions</title>
           |  <dateCreated>$formattedTimestamp</dateCreated>
           |</head>
           |""".stripMargin)
    }

    def writeBody()(implicit writer: Writer): Unit = {
      writer.write("<body>\r\n")
      subscriptions foreach { case (title, url) =>
        val encodedTitle = Text(title)
        val encodedUrl = Text(url.toString)
        writer.write(s"""  <outline type="rss" text="$encodedTitle" xmlUrl="$encodedUrl" />\r\n""")
      }
      writer.write("</body>\r\n")
    }

    def writeClosing()(implicit writer: Writer): Unit =
      writer.write("</opml>")

    forCloseable (new BufferedWriter(new FileWriter(file, false))) { implicit writer =>
      writePreamble()
      writeHead()
      writeBody()
      writeClosing()
    }
  }
}
