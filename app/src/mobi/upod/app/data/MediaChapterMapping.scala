package mobi.upod.app.data

import mobi.upod.data.{Mapping, MappingProvider}
import mobi.upod.media.{ChapterImageReference, ChapterWebLink, MediaChapter}

object MediaChapterMapping extends MappingProvider[MediaChapter] {

  import Mapping._

  object LinkMapping extends MappingProvider[ChapterWebLink] {
    override val mapping: Mapping[ChapterWebLink] = map(
      "url" -> string,
      "title" -> optional(string)
    )(ChapterWebLink.apply)(ChapterWebLink.unapply)
  }

  object ImageReferenceMapping extends MappingProvider[ChapterImageReference] {
    override val mapping: Mapping[ChapterImageReference] = map(
      "mimeType" -> string,
      "offset" -> long,
      "length" -> long
    )(ChapterImageReference.apply)(ChapterImageReference.unapply)
  }

  override val mapping: Mapping[MediaChapter] = map(
    "startMillis" -> long,
    "endMillis" -> long,
    "title" -> optional(string),
    "link" -> optional(LinkMapping),
    "image" -> optional(ImageReferenceMapping)
  )(MediaChapter.apply)(MediaChapter.unapply)
}