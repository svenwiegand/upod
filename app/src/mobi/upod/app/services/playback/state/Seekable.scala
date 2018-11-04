package mobi.upod.app.services.playback.state

import mobi.upod.app.services.playback.player.MediaPlayer
import mobi.upod.app.services.playback.player.MediaPlayer.OnSeekCompleteListener
import mobi.upod.media.{MediaChapter, MediaChapterTable}

import scala.util.Try


private[playback] trait Seekable extends PlaybackState with StateWithUpdatableEpisode with OnSeekCompleteListener {
  val chapters: MediaChapterTable
  private var _currentChapter: Option[MediaChapter] = chapters.chapterAt(player.getCurrentPosition)

  def currentChapter: Option[MediaChapter] = _currentChapter

  override protected[state] def onEnterState() {
    super.onEnterState()
    player.setOnSeekCompleteListener(this)
  }

  override protected[state] def onExitState() {
    Try(player.setOnSeekCompleteListener(null))
    super.onExitState()
  }

  def canSkipChapter: Boolean = chapters.hasNextChapter(player.getCurrentPosition)

  def skipChapter(): Boolean = {
    val nextChapter = chapters.nextChapter(player.getCurrentPosition)
    nextChapter.foreach(c => seek(c.startMillis.toInt, true))
    nextChapter.nonEmpty
  }

  def canGoBackChpter: Boolean = chapters.hasBackChapter(player.getCurrentPosition)

  def goBackChapter(): Boolean = {
    val backChapter = chapters.chapterBack(player.getCurrentPosition)
    backChapter.foreach(c => seek(c.startMillis.toInt, true))
    backChapter.nonEmpty
  }

  def seek(position: Int, commit: Boolean) {
    player.seekTo(Math.max(Math.min(position, player.getDuration), 0), commit)
  }

  def onSeekComplete(mediaPlayer: MediaPlayer) {
    log.debug(s"onSeekComplete()")
    updateProgress(true)
  }

  override protected def updateProgress(forcePersist: Boolean): Unit = {
    super.updateProgress(forcePersist)
    fireIfChapterChanged()
  }

  private def fireIfChapterChanged(): Unit = {
    val chapter = chapters.chapterAt(player.getCurrentPosition)
    if (chapter != _currentChapter) {
      _currentChapter = chapter
      fire(_.onCurrentChapterChanged(chapter))
    }
  }
}
