package mobi.upod.app.services.playback

import android.app.PendingIntent
import android.content.Intent
import com.escalatesoft.subcut.inject.{BindingModule, Injectable}
import mobi.upod.android.media.PlaybackState._
import mobi.upod.android.media.{MediaSessionCompat, PlaybackState}
import mobi.upod.android.os.AsyncTask
import mobi.upod.app.data.EpisodeBaseWithPlaybackInfo
import mobi.upod.app.gui.CoverartPlaceholderDrawable
import mobi.upod.app.gui.playback.PlaybackActivity
import mobi.upod.app.storage.{CoverartProvider, ImageSize}

private[playback] class MediaSession(service: PlaybackServiceImpl)(implicit val bindingModule: BindingModule)
  extends PlaybackListener
  with MediaSessionCompat.Callback
  with Injectable {

  private lazy val coverartProvider = inject[CoverartProvider]
  private lazy val coverartPlaceholderDrawable = new CoverartPlaceholderDrawable
  private lazy val mediaSession = createMediaSession

  private def createMediaSession = {
    val intent = new Intent(Intent.ACTION_MEDIA_BUTTON)
    intent.setComponent(MediaControlEventReceiver.componentName(service))
    val activityIntent = PendingIntent.getActivity(service, 0, PlaybackActivity.intent(service), 0)
    val mediaButtonIntent = PendingIntent.getBroadcast(service, 0, intent, 0)
    val session = MediaSessionCompat(service, activityIntent, mediaButtonIntent)
    session.setCallback(Some(this))
    session
  }

  def register(): Unit = {
    MediaControlEventReceiver.register(service)
    mediaSession.register()
    service.addSynchronousListener(this)
  }

  def unregister(): Unit = {
    service.removeSynchronousListener(this)
    mediaSession.unregister()
    MediaControlEventReceiver.unregister(service)
  }

  def release(): Unit =
    mediaSession.release()

  def token: Option[MediaSessionCompat.Token] =
    mediaSession.token

  //
  // MediaSession Callback

  override def onPause(): Unit =
    service.pause()

  override def onSkip(): Unit =
    service.skip()

  override def onPlay(): Unit =
    service.resume()

  override def onSeekTo(pos: Long): Unit =
    service.seek(pos, true)

  override def onFastForward(): Unit =
    service.fastForward()

  override def onRewind(): Unit =
    service.rewind()

  override def onStop(): Unit =
    service.stop()

  //
  // playback listener
  //

  private def setPlaybackState(state: PlaybackState): Unit =
    mediaSession.setPlaybackState(state, service.canResume, service.canPause, service.canSeek, service.canStop)

  override def onPreparingPlayback(episode: EpisodeBaseWithPlaybackInfo): Unit =
    setPlaybackState(Buffering)

  override def onPlaybackStarted(episode: EpisodeBaseWithPlaybackInfo): Unit =
    setPlaybackState(Playing(service.position, service.playbackSpeedMultiplier))

  override def onPlaybackPaused(episode: EpisodeBaseWithPlaybackInfo): Unit =
    setPlaybackState(Paused(service.position))

  override def onPlaybackStopped(): Unit =
    setPlaybackState(Stopped)

  override def onEpisodeChanged(episode: Option[EpisodeBaseWithPlaybackInfo]): Unit =
    episode.foreach(e => AsyncTask.execute(updateMetaData(e)))

  private def updateMetaData(episode: EpisodeBaseWithPlaybackInfo): Unit = {
    coverartPlaceholderDrawable.set(episode.podcastInfo.title, episode.extractedOrGeneratedColors)
    val coverart = episode.podcastInfo.imageUrl match {
      case Some(imageUrl) =>
        coverartProvider.getImageOrPlaceholderBitmap(service, imageUrl, ImageSize.largestScreenDimension, coverartPlaceholderDrawable)
      case None =>
        coverartProvider.getPlaceholderBitmap(service, ImageSize.largestScreenDimension, coverartPlaceholderDrawable)
    }

    mediaSession.setMetaData(episode.podcastInfo.title, episode.title, episode.media.duration, coverart)
  }
}
