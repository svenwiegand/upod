package mobi.upod.media.mp3

import java.io.File

import mobi.upod.media.{MediaChapterTable, MediaChapter}

/** Reads chapter frames from an ID3v2 tag of an MP3 file.
  *
  * Based on the specs at http://id3.org/id3v2.3.0 http://id3.org/id3v2-chapters-1.0
  */
private[media] object Mp3ChapterRetriever {

  def parse(file: File): MediaChapterTable = {
    val mp3 = new Mp3File(file)
    try {
      if (mp3.readId3Tag()) {
        MediaChapterTable(readChapters(mp3).toIndexedSeq)
      } else {
        MediaChapterTable()
      }
    } finally mp3.close()
  }

  private def readChapters(f: Mp3File): Seq[MediaChapter] = {
    val frames = f.readFrames()
    frames.collect { case ChapterFrame(_, c) => c }
  }
}
