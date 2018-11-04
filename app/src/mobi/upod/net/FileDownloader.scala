package mobi.upod.net

import java.io.{InputStream, RandomAccessFile, File}
import java.net.{HttpURLConnection, URLConnection, URL}
import mobi.upod.io._
import mobi.upod.util.Duration.IntDuration
import mobi.upod.util.StorageSize._
import mobi.upod.android.logging.Logging

object FileDownloader extends Logging {
  val ReadBufferSize = 8.kb
  val ConnectTimeout = 30.seconds
  val ReadTimeout = 1.minute

  case class Progress(downloaded: Long, length: Long, bytesPerSecond: Int, remainingMillis: Option[Long])

  def download(
      resource: URL,
      file: File,
      downloadOffset: Long,
      expectedLength: Long,
      allowNewFileOrFail: Long => Unit,
      listen: Progress => Unit,
      canceled: => Boolean): Progress = {

    log.crashLogInfo(s"downloading $resource to $file")
    val requestOffset = Math.min(downloadOffset, file.length)
    val connection = connect(resource, requestOffset)
    try {
      val (offset, length) = prepare(file, connection, requestOffset, expectedLength)
      if (!file.exists()) {
        allowNewFileOrFail(length)
      }
      download(connection, file, offset, length)(listen, () => canceled)
    } finally {
      connection.disconnect()
    }
  }

  private def connect(resource: URL, requestOffset: Long): HttpURLConnection = {
    val connection = new HttpURLConnector(resource).
      setConnectTimeout(ConnectTimeout).
      setReadTimeout(ReadTimeout).
      setRequestProperty("User-Agent", UserAgent.Name).
      setRequestProperty("Range", s"bytes=$requestOffset-").
      connect()
    try {
      HttpConnectException.throwOnFailure(connection.connect())

      connection.getResponseCode match {
        case status if status < 300 =>
          connection
        case status =>
          throw new HttpStatusException(status)
      }
    } catch {
      case ex: HttpStatusException =>
        connection.disconnect()
        throw ex
    }
  }

  private def prepare(file: File, connection: URLConnection, requestOffset: Long, expectedLength: Long): (Long, Long) = {
    val remainingLength = connection.getContentLength
    log.crashLogInfo(s"remaining length: $remainingLength")
    val newDownload =
      requestOffset == 0 ||
      remainingLength < 0 ||
      remainingLength >= expectedLength
    if (newDownload) {
      file.delete()
    }
    log.crashLogInfo(s"new download: $newDownload")
    val offset = if (newDownload) 0 else requestOffset
    val length = if (newDownload && remainingLength > 0) remainingLength else expectedLength
    (offset, length)
  }

  private def prepareTargetFile(file: RandomAccessFile, offset: Long, length: Long) {
    if (length > 0 && file.length < length) {
      file.seek(length - 1)
      file.write(0)
    }
    file.seek(offset)
  }

  private def Progress(overallDownloadedBytes: Long, bytesDownloadedSinceStart: Long, length: Long, startTime: Long): Progress = {
    val currentTime = System.currentTimeMillis()

    val bytesPerSecond: Int = if (currentTime > startTime)
      (1000 * bytesDownloadedSinceStart / (currentTime - startTime)).toInt
    else
      bytesDownloadedSinceStart.toInt

    val remainingBytes = Math.max(0, length - overallDownloadedBytes)
    val remainingMillis = if (length > 0 && bytesPerSecond > 0) Some(1000 * remainingBytes / bytesPerSecond) else None

    Progress(overallDownloadedBytes, length, bytesPerSecond, remainingMillis)
  }

  private def downloadBlock(
      src: InputStream,
      dest: RandomAccessFile,
      position: Long,
      length: Long,
      downloadedBytes: Long = 0)(
      implicit listen: Progress => Unit,
      canceled: () => Boolean,
      startTime: Long = System.currentTimeMillis(),
      buffer: Array[Byte] = new Array[Byte](ReadBufferSize)): Progress = {

    if (!canceled()) {
      val progress = Progress(position, downloadedBytes, length, startTime)
      if (downloadedBytes > 0) {
        listen(progress)
      }
      HttpConnectException.throwOnFailure(src.read(buffer)) match {
        case byteCount if byteCount >= 0 =>
          dest.write(buffer, 0, byteCount)
          downloadBlock(src, dest, position + byteCount, length, downloadedBytes + byteCount)
        case _ =>
          progress
      }
    } else {
      throw new InterruptedException("download has been interrupted")
    }
  }

  private def download(
      connection: URLConnection,
      file: File,
      offset: Long,
      length: Long)(
      implicit listen: Progress => Unit,
      canceled: () => Boolean): Progress = {

    forCloseable(HttpConnectException.throwOnFailure(connection.getInputStream), new RandomAccessFile(file, "rw")) { (src, dest) =>
      prepareTargetFile(dest, offset, length)
      downloadBlock(src, dest, offset, length)
    }
  }
}
