package mobi.upod.media.mp4

import com.coremedia.iso.IsoFile
import com.coremedia.iso.boxes.TrackBox

import scala.collection.JavaConverters._

class TrackIndex(val tracks: IndexedSeq[Track]) {

  def find(handlerType: String): Option[Track] =
    tracks.find(_.handlerType == handlerType)

  def find(handlerType: String, name: String): Option[Track] =
    tracks.find(t => t.handlerType == handlerType && t.name.toLowerCase == ('"' + name + '"'))

  def findFirstTextTrack: Option[Track] = find(Track.HandlerType.Text)

  def findFirstVideoTrack: Option[Track] = find(Track.HandlerType.Video)

  def findExplicitChapterTitleTrack: Option[Track] =
    find(Track.HandlerType.Text, "chapter titles")

  def findChapterTitleOrFirstTextTrack: Option[Track] =
    findExplicitChapterTitleTrack orElse findFirstTextTrack

  def findExplicitChapterImageTrack: Option[Track] =
    find(Track.HandlerType.Text, "chapter images")

  def findChapterImageOrFirstVideoTrack: Option[Track] =
    findExplicitChapterImageTrack orElse findFirstVideoTrack

  def findExplicitChapterLinkTrack: Option[Track] =
    find(Track.HandlerType.Text, "chapter urls")

}

object TrackIndex {

  def read(file: IsoFile): TrackIndex = {
    val movieBox = file.getMovieBox
    val tracks = movieBox.getBoxes().asScala collect { case t: TrackBox => new Track(t) }
    new TrackIndex(tracks.toIndexedSeq)
  }
}