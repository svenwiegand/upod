package mobi.upod.app.services

import android.app.{Notification, PendingIntent}
import android.content.{Context, Intent}
import android.graphics.Bitmap
import android.support.v4.media.session.MediaSessionCompat
import android.support.v7.app.NotificationCompat
import com.escalatesoft.subcut.inject.{BindingModule, Injectable}
import mobi.upod.android.app.AppNotificationBuilder
import mobi.upod.app.App
import mobi.upod.app.data.EpisodeBase
import mobi.upod.app.gui.{CoverartPlaceholderDrawable, MainActivity}
import mobi.upod.app.storage.{CoverartProvider, ImageSize}
import mobi.upod.util.{Builder, BuilderObject}

class EpisodeNotificationBuilder(context: Context, ongoing: Boolean = true)(implicit val bindingModule: BindingModule)
  extends Builder[Notification]
  with Injectable {
  implicit private val ctx = context
  private val coverartProvider = inject[CoverartProvider]
  private val builder = new AppNotificationBuilder(context)
  private val coverartPlaceholderDrawable = new CoverartPlaceholderDrawable

  builder.setOngoing(ongoing)
  builder.setShowWhen(!ongoing)

  def setAutoCancel(autoCancel: Boolean = true): EpisodeNotificationBuilder ={
    builder.setAutoCancel(autoCancel)
    this
  }

  def setIcon(iconId: Int): EpisodeNotificationBuilder = {
    builder.setSmallIcon(iconId)
    this
  }

  def setEpisode(episode: EpisodeBase): EpisodeNotificationBuilder =  {
    builder.setContentTitle(episode.title)
    builder.setLargeIcon(episodeImage(episode))

    val color = episode.extractedOrGeneratedColors.nonLightBackground
    builder.setColor(color)
    this
  }

  private def episodeImage(episode: EpisodeBase): Bitmap = {
    coverartPlaceholderDrawable.set(episode.podcastInfo.title, episode.extractedOrGeneratedColors)
    episode.podcastInfo.imageUrl match {
      case Some(url) => coverartProvider.getImageOrPlaceholderBitmap(context, url, ImageSize.list, coverartPlaceholderDrawable)
      case None => coverartProvider.getPlaceholderBitmap(context, ImageSize.list, coverartPlaceholderDrawable)
    }
  }

  def setContentText(text: String): EpisodeNotificationBuilder =  {
    builder.setContentText(text)
    this
  }

  def setIndeterminateProgress(): EpisodeNotificationBuilder =  {
    builder.setProgress(0, 0, true)
    this
  }

  def setProgress(progress: Int, max: Int): EpisodeNotificationBuilder =  {
    builder.setProgress(max, progress, false)
    this
  }

  def setIntent(intent: PendingIntent) = {
    builder.setContentIntent(intent)
    this
  }

  def setActivity(intent: Intent, requestCode: Int = 0) = {
    setIntent(PendingIntent.getActivity(inject[App], requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT))
    this
  }

  def setTargetNavigationItem(navItemId: Long, viewModeId: Int = 0) = {
    setActivity(MainActivity.intent(inject[App], navItemId, viewModeId), navItemId.toInt)
    this
  }

  def addAction(icon: Int, title: Int, intent: PendingIntent): EpisodeNotificationBuilder = {
    addAction(icon, context.getString(title), intent)
    this
  }

  def addAction(icon: Int, title: String, intent: PendingIntent): EpisodeNotificationBuilder = {
    builder.addAction(icon, title, intent)
    this
  }

  def setMediaStyle(session: Option[MediaSessionCompat.Token], actionsShownInCompactView: Int*): EpisodeNotificationBuilder = {
    val mediaStyle = new NotificationCompat.MediaStyle()
    mediaStyle.setMediaSession(session.orNull)
    mediaStyle.setShowActionsInCompactView(actionsShownInCompactView: _*)
    builder.setStyle(mediaStyle)
    this
  }

  def build = builder.build
}

object EpisodeNotificationBuilder extends BuilderObject[Notification]