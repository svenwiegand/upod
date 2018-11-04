package mobi.upod.io

import java.io.{BufferedInputStream, InputStream}

import scala.annotation.tailrec

class RichInputStream(val stream: InputStream) extends AnyVal {

  def buffered: BufferedInputStream = stream match {
    case s: BufferedInputStream => s
    case _ => new BufferedInputStream(stream)
  }

  @tailrec
  final def skipWhile(condition: Byte => Boolean): Unit = {
    val input = stream.read()
    if (input >= 0 && condition(input.toByte)) skipWhile(condition)
  }

  final def readString(): String = {
    @tailrec
    def readNextChar(str: StringBuilder): String = {
      val input = stream.read()
      if (input == 0 || input == -1)
        str.toString()
      else {
        str.append(input)
        readNextChar(str)
      }
    }

    val str = new StringBuilder
    readNextChar(str)
  }

  final def readUnsignedInt8(): Int =
    stream.read()

  final def readUnsignedInt32(): Long = {
    ((0xff & stream.read()) << 24) | ((0xff & stream.read()) << 16) |
      ((0xff & stream.read()) << 8) | (0xff & stream.read())
  }
}

object RichInputStream {

  implicit def richInputStream(stream: InputStream): RichInputStream =
    new RichInputStream(stream)
}