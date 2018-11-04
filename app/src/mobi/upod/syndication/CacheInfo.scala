package mobi.upod.syndication

import java.net.URLConnection

import mobi.upod.net.HttpURLConnector
import org.joda.time.DateTime

case class CacheInfo(eTag: Option[String], lastModified: Option[DateTime]) {

  def applyToConnection(connection: URLConnection): URLConnection = {
    lastModified.foreach(timestamp => connection.setIfModifiedSince(timestamp.getMillis))
    eTag.foreach(tag => connection.setRequestProperty("If-None-Match", tag))
    connection
  }

  def applyToConnector(connector: HttpURLConnector): HttpURLConnector = {
    lastModified.foreach(timestamp => connector.setIfModifiedSince(timestamp.getMillis))
    eTag.foreach(tag => connector.setRequestProperty("If-None-Match", tag))
    connector
  }
}

object CacheInfo {

  val empty = CacheInfo(None, None)

  def apply(connection: URLConnection): CacheInfo = {
    val eTag = connection.getHeaderField("ETag")
    val lastModified = connection.getLastModified
    CacheInfo(Option(eTag), if (lastModified > 0) Some(new DateTime(lastModified)) else None)
  }
}
