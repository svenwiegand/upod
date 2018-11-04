package mobi.upod.media

import java.io.File

import mobi.upod.media.mp3.Mp3ChapterRetriever
import mobi.upod.media.mp4.Mp4ChapterRetriever

object MediaChapterRetriever {

  def parse(file: File, mimeType: String): MediaChapterTable = mimeType.toUpperCase match {
    case ("AUDIO/MP3" | "AUDIO/MPEG") => Mp3ChapterRetriever.parse(file)
    case ("AUDIO/MP4" | "AUDIO/AAC" | "AUDIO/M4A" | "AUDIO/X-M4A") => Mp4ChapterRetriever.parse(file, true)
    case "VIDEO/MP4" => Mp4ChapterRetriever.parse(file)
    case _ => MediaChapterTable()
  }

  def parse(file: File, mimeType: String, fallbackMimeType: Option[String]): MediaChapterTable = {
    val chapterTable = parse(file, mimeType)
    if (chapterTable.isEmpty && fallbackMimeType.nonEmpty)
      parse(file, fallbackMimeType.get)
    else
      chapterTable
  }
}