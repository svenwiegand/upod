package mobi.upod.media

case class ChapterWebLink(url: String, title: Option[String])

case class ChapterImageReference(mimeType: String, offset: Long, length: Long)

/** Represents a chapter in a media file
  *
  * @param startMillis start position of the chapter (including)
  * @param endMillis end position of the chapter (excluding)
  * @param title title of the chapter
  * @param link link related to the chapter
  * @param image image related to the chapter
  */
case class MediaChapter(
  startMillis: Long,
  endMillis: Long,
  title: Option[String],
  link: Option[ChapterWebLink],
  image: Option[ChapterImageReference]
) {

  def duration: Long = endMillis - startMillis
}
