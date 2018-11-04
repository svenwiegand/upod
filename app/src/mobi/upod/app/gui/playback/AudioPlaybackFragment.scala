package mobi.upod.app.gui.playback

import android.graphics.Bitmap
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import mobi.upod.android.graphics.Color
import mobi.upod.app.R
import mobi.upod.app.data.EpisodeListItem
import mobi.upod.app.gui.chapters.{ChapterImageActivity, ChapterImageLoader}
import mobi.upod.app.storage.{StoragePreferences, ImageSize}
import mobi.upod.media.{MediaChapterTable, ChapterImageReference, MediaChapter}

final class AudioPlaybackFragment extends PlaybackFragment {
  private lazy val storagePreferences = inject[StoragePreferences]
  override protected val podcastImageSize: ImageSize = ImageSize.smallestScreenDimension
  private lazy val currentChapterImageView = childImageView(R.id.currentChapterImage)
  private var _currentChapter: Option[MediaChapter] = None

  protected def viewId = R.layout.audio_playback

  override def onActivityCreated(savedInstanceState: Bundle): Unit = {
    super.onActivityCreated(savedInstanceState)
    currentChapterImageView.onClick(showFullScreenChapterImage())
  }

  override protected def updateActionBarBackground(color: Color): Unit = {
    supportActionBar.setBackgroundDrawable(new ColorDrawable(color))
    supportActionBar.setElevation(0)
  }

  //
  // chapter image stuff
  //

  override protected def onChaptersChanged(episode: EpisodeListItem, chapters: MediaChapterTable): Unit = {
    super.onChaptersChanged(episode, chapters)
    onCurrentChapterChanged(chapters.chapterAt(episode.playbackInfo.playbackPosition))
  }

  override def onCurrentChapterChanged(chapter: Option[MediaChapter]): Unit = {
    super.onCurrentChapterChanged(chapter)
    _currentChapter = chapter
    updateCurrentChapterImage(chapter.flatMap(_.image))
  }

  private def updateCurrentChapterImage(imageReference: Option[ChapterImageReference]): Unit = {

    def showChapterImage(img: Bitmap): Unit = {
      currentChapterImageView.setImageBitmap(img)
      currentChapterImageView.show(true)
      podcastImageView.foreach(_.show(false))
    }

    def showPodcastImage(): Unit = {
      currentChapterImageView.setImageDrawable(null)
      podcastImageView.foreach(_.show(true))
      currentChapterImageView.show(false)
    }

    val file = episode.map(_.mediaFile(storagePreferences.storageProvider))
    (imageReference, file) match {
      case (Some(img), Some(f)) if f.exists =>
        ChapterImageLoader.asyncLoadImage(f, img, showChapterImage, _ => showPodcastImage())
      case _ =>
        showPodcastImage()
    }
  }

  private def showFullScreenChapterImage(): Unit = {
    val file = episode.map(_.mediaFile(storagePreferences.storageProvider))
    (_currentChapter, file) match {
      case (Some(chapter), Some(f)) => ChapterImageActivity.start(getActivity, chapter, f)
      case _ => // ignore
    }
  }
}
