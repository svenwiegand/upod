package mobi.upod.media.mp4

import java.io.{File, RandomAccessFile}

import com.coremedia.iso.IsoFile
import mobi.upod.media.{MediaChapterTable, ChapterWebLink, ChapterImageReference, MediaChapter}

private[media] object Mp4ChapterRetriever {
  private val SampleStartTolerance = 500l

  def parse(file: File, videoStreamAsChapterImages: Boolean = false): MediaChapterTable = {
    val isoFile = new IsoFile(file.getPath)
    try {
      val tracks = TrackIndex.read(isoFile)
      val src = new RandomAccessFile(file, "r")
      try MediaChapterTable(parseChapters(tracks, src, videoStreamAsChapterImages)) finally src.close()
    } finally isoFile.close()
  }

  private def parseChapters(tracks: TrackIndex, src: RandomAccessFile, videoStreamAsChapterImages: Boolean): IndexedSeq[MediaChapter] = {
    val chapters = findChapterTitles(tracks, src) map { chapterTitles =>
      val chapterLinks = findChapterLinks(tracks, src)
      val chapterImages = findChapterImages(tracks, videoStreamAsChapterImages)
      val allChapters = buildChapters(chapterTitles, chapterLinks, chapterImages)
      allChapters.dropWhile(c => (c.title.isEmpty || c.title.contains("")) &&  c.link.isEmpty && c.image.isEmpty)
    }
    chapters getOrElse IndexedSeq()
  }

  private def findChapterTitles(tracks: TrackIndex, src: RandomAccessFile): Option[SampleTable[StringSample]] =
    tracks.findChapterTitleOrFirstTextTrack.map(SampleTable.read(src, _, StringSampleReader))

  private def findChapterLinks(tracks: TrackIndex, src: RandomAccessFile): Option[SampleTable[LinkSample]] = {
    val linkTable = tracks.findExplicitChapterLinkTrack.map(SampleTable.read(src, _, LinkSampleReader))
    linkTable map { table =>
      SampleTable(table.samples.filterNot(_.href.isEmpty))
    }
  }

  private def findChapterImages(tracks: TrackIndex, videoStreamAsChapterImages: Boolean): Option[SampleTable[SampleInfo]] = {
    val chapterImageTrack = if (videoStreamAsChapterImages) tracks.findChapterImageOrFirstVideoTrack else tracks.findExplicitChapterImageTrack
    chapterImageTrack.map(_.sampleInfoTable)
  }

  private def buildChapters(
    chapterTitles: SampleTable[StringSample],
    chapterLinks: Option[SampleTable[LinkSample]],
    chapterImages: Option[SampleTable[SampleInfo]]): IndexedSeq[MediaChapter] = {

    chapterTitles.samples map { chapterTitle =>
      val link = chapterLinks.flatMap(_.sampleStartingAt(chapterTitle.startMillis, SampleStartTolerance).map(l => ChapterWebLink(l.href, Option(l.ankerText))))
      val imgRef = chapterImages.flatMap(_.sampleStartingAt(chapterTitle.startMillis, SampleStartTolerance).map(si => ChapterImageReference("image", si.fileOffset, si.length)))
      MediaChapter(chapterTitle.startMillis, chapterTitle.endMillis, Some(chapterTitle.str), link, imgRef)
    }
  }
}
