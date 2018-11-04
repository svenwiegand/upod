package mobi.upod.media.mp3

import java.io.{File, RandomAccessFile}
import java.nio.charset.Charset

import scala.annotation.tailrec
import scala.collection.mutable.ArrayBuffer


private[mp3] class IsoMediaFile(f: File) extends RandomAccessFile(f, "r") {
  protected val CHARSET_DEFAULT = Charset.forName("ISO-8859-1")
  protected val CHARSET_UNICODE = Charset.forName("UTF-16")

  def isEndOfFile: Boolean =
    getFilePointer >= length

  def skip(n: Long): Unit = {
    val fullInts = n / Int.MaxValue
    for (i <- 1 to fullInts.toInt) {
      skipBytes(Int.MaxValue)
    }

    val rest = n % Int.MaxValue
    skipBytes(rest.toInt)
  }

  def readUnsignedInt(): Long = {
    val (b3, b2, b1, b0) = (readUnsignedByte(), readUnsignedByte(), readUnsignedByte(), readUnsignedByte())
    ((b3 & 0xff).toLong << 24) | ((b2 & 0xff) << 16) | ((b1 & 0xff) << 8) | b0
  }

  def readIsoString(): String = {
    val bytes = new ArrayBuffer[Byte]

    @tailrec
    def readRemainingBytes(): Unit = {
      val byte = readByte()
      if (byte > 0) {
        bytes += byte
        readRemainingBytes()
      }
    }

    readRemainingBytes()
    new String(bytes.toArray, CHARSET_DEFAULT)
  }

  def readUnicodeString(): String = {
    val bytes = new ArrayBuffer[Byte]

    @tailrec
    def readRemainingBytes(): Unit = {
      (readByte(), readByte()) match {
        case (0, 0) => // end of string
        case (b0, b1) =>
          bytes.append(b0, b1)
          readRemainingBytes()
      }
    }

    readRemainingBytes()
    new String(bytes.toArray, CHARSET_UNICODE)
  }

  def readEncodedString(encoding: Byte): String = encoding match {
    case 0x01 => readUnicodeString()
    case _ => readIsoString()
  }
  
  /** Interprets the first read byte as encoding flag and reads and returns the following text accordingly */
  def readEncodedString(): String = 
    readEncodedString(readByte())

  def readString(size: Long, charset: Charset): String = {
    val bytes = new Array[Byte](size.toInt)
    read(bytes)
    new String(bytes, charset)
  }

  def readIsoString(size: Long): String =
    readString(size, CHARSET_DEFAULT)

  def readUnicodeString(size: Long): String =
    readString(size, CHARSET_UNICODE)

  def readEncodedString(encoding: Byte, size: Long): String = encoding match {
    case 0x01 => readUnicodeString(size)
    case _ => readIsoString(size)
  }

  def readEncodedString(size: Long): String =
    readEncodedString(readByte(), size - 1)
}
