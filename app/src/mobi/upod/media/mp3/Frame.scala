package mobi.upod.media.mp3

import mobi.upod.media.{MediaChapter, ChapterImageReference, ChapterWebLink}

import scala.util.matching.Regex

//
// abstract frame spec
//

private[mp3] case class FrameHeader(id: String, size: Long, flags: Short)

private[mp3] abstract class Frame {
  val header: FrameHeader
}

private[mp3] trait FrameReader[A <: Frame] {
  val Pattern: Regex

  def read(f: Mp3File, header: FrameHeader): A
}

//
// ignored frame spec
//

private[mp3] case class IgnoredFrame(header: FrameHeader) extends Frame

private[mp3] object IgnoredFrameReader extends FrameReader[IgnoredFrame] {
  val Pattern = "[0-9A-Z]{4}".r

  override def read(f: Mp3File, header: FrameHeader): IgnoredFrame = {
    f.skip(header.size)
    IgnoredFrame(header)
  }
}

//
// chapter frame spec
//

private[mp3] case class ChapterFrame(header: FrameHeader, chapter: MediaChapter) extends Frame

private[mp3] object ChapterFrameReader extends FrameReader[ChapterFrame] {

  override val Pattern: Regex = "CHAP".r

  override def read(f: Mp3File, header: FrameHeader): ChapterFrame = {
    val frameEnd = f.getFilePointer + header.size
    f.readIsoString() // element ID
    val startTime = f.readUnsignedInt()
    val endTime = f.readUnsignedInt()
    f.readUnsignedInt() // start offset
    f.readUnsignedInt() // end offset
    val subFrames = f.readFramesUntil(frameEnd)
    f.seek(frameEnd)

    val title = subFrames.collectFirst { case TextFrame(FrameHeader("TIT2", _, _), txt) => txt }
    val link = subFrames.collectFirst { case UserLinkFrame(_, lnk) => lnk}
    val image = subFrames.collectFirst { case ImageFrame(_, img) => img }
    val content = MediaChapter(startTime, endTime, title, link, image)
    ChapterFrame(header, content)
  }
}

//
// text information frame spec
//

private[mp3] case class TextFrame(header: FrameHeader, text: String) extends Frame

private[mp3] object TextFrameReader extends FrameReader[TextFrame] {

  override val Pattern: Regex = "(?!^TXXX$)(?:^T[0-9A-Z]{3}$)".r

  override def read(f: Mp3File, header: FrameHeader): TextFrame = {
    val text = f.readEncodedString(header.size)
    new TextFrame(header, text)
  }
}

//
// user defined URL link
//

private[mp3] case class UserLinkFrame(header: FrameHeader, link: ChapterWebLink) extends Frame

private[mp3] object UserLinkFrameReader extends FrameReader[UserLinkFrame] {

  override val Pattern: Regex = "WXXX".r

  override def read(f: Mp3File, header: FrameHeader): UserLinkFrame = {
    val contentStart = f.getFilePointer
    val description = f.readEncodedString()

    val urlLength = header.size - (f.getFilePointer - contentStart)
    val url = f.readIsoString(urlLength)
    new UserLinkFrame(header, ChapterWebLink(url, Some(description)))
  }
}

//
// attached picture frame
//

private[mp3] case class ImageFrame(header: FrameHeader, image: ChapterImageReference) extends Frame

private[mp3] object ImageFrameReader extends FrameReader[ImageFrame] {
  override val Pattern: Regex = "APIC".r
  private val MimeTypePattern: Regex = "(.*/.*)".r

  override def read(f: Mp3File, header: FrameHeader): ImageFrame = {
    val contentStart = f.getFilePointer
    val encoding = f.readByte()
    val mimeType = f.readIsoString() match {
      case MimeTypePattern(mt) => mt
      case imageType => s"image/$imageType"
    }
    f.readByte() // picture type
    f.readEncodedString(encoding) // description
    val offset = f.getFilePointer
    val length = header.size - (offset - contentStart)
    f.seek(offset + length)

    val img = ChapterImageReference(mimeType, offset, length)
    new ImageFrame(header, img)
  }
}