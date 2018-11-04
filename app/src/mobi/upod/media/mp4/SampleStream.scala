package mobi.upod.media.mp4

import java.io.DataInput

class SampleStream(private val stream: DataInput) extends AnyVal {

  def readByte(): Byte = stream.readByte()

  def readUnsignedByte(): Int = stream.readByte()

  def readShort(): Short = stream.readShort()

  def readUnsignedShort(): Int = stream.readUnsignedShort()

  def readInt(): Int = stream.readInt()

  def readLong(): Long = stream.readLong()

  def read(b: Array[Byte]): Unit = stream.readFully(b)

  def readBytes(len: Int): Array[Byte] = {
    val bytes = new Array[Byte](len)
    read(bytes)
    bytes
  }

  def readString(len: Int): String =
    new String(readBytes(len), "UTF-8")

  def skipBytes(n: Int): Unit = stream.skipBytes(n)
}
