package mobi.upod.media.mp3

import java.io.File

import scala.annotation.tailrec


private[mp3] class Mp3File(f: File) extends IsoMediaFile(f) {

  def readId3Tag(): Boolean = {
    //                 I       D       3      v1      v0      fl      s3      s2      s1      s0
    val tagHeader = (read(), read(), read(), read(), read(), read(), read(), read(), read(), read())
    tagHeader match {
      case (0x49, 0x44, 0x33, v1, v0, fl, s3, s2, s1, s0) if v1 < 0xff && v1 < 0xff && s3 < 0x80 && s2 < 0x80 && s1 < 0x80 && s0 < 0x80 => true
      case _ => false
    }
  }

  def readFrameHeader(): Option[FrameHeader] =
    readId().map(FrameHeader(_, readUnsignedInt(), readShort()))

  def readFrame(): Option[Frame] = {
    readFrameHeader() map { header =>
      val reader: FrameReader[_ <: Frame] = header.id match {
        case ChapterFrameReader.Pattern() => ChapterFrameReader
        case TextFrameReader.Pattern() => TextFrameReader
        case UserLinkFrameReader.Pattern() => UserLinkFrameReader
        case ImageFrameReader.Pattern() => ImageFrameReader
        case _ => IgnoredFrameReader
      }
      reader.read(this, header)
    }
  }

  def readFramesUntil(filePos: Long): Seq[Frame] = {
    val frames = new collection.mutable.ListBuffer[Frame]

    @tailrec
    def readRemainingFrames(): Unit = if (getFilePointer < filePos) {
      readFrame() match {
        case Some(frame) =>
          frames += frame
          readRemainingFrames()
        case None => // we're done
      }
    }

    readRemainingFrames()
    frames.toSeq
  }

  def readFrames(): Seq[Frame] =
    readFramesUntil(length())

  def readId(): Option[String] = {
    val idBytes = new Array[Byte](4)
    read(idBytes)
    val id = idBytes.filter(b => (b >= '0' && b <= '9') || (b >= 'A' && b <= 'Z'))
    if (id.length == 4)
      Some(new String(idBytes, CHARSET_DEFAULT))
    else
      None
  }
}
