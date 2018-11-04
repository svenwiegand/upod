package mobi.upod.app.gui.playback

import android.content.pm.ActivityInfo
import android.graphics.drawable.{ColorDrawable, BitmapDrawable}
import android.graphics.{Bitmap, SurfaceTexture}
import android.os.Bundle
import android.support.v7.app.ActionBarActivity
import android.view.TextureView.SurfaceTextureListener
import android.view._
import android.widget.FrameLayout.LayoutParams
import android.widget.SeekBar
import mobi.upod.android.app.FragmentStateHolder
import mobi.upod.android.graphics.Color
import mobi.upod.android.os.AsyncTask
import mobi.upod.android.util.ApiLevel
import mobi.upod.app.R
import mobi.upod.app.data.{EpisodeListItem, EpisodeBaseWithPlaybackInfo}
import mobi.upod.app.services.playback.VideoSize
import mobi.upod.app.storage.{StoragePreferences, ImageSize, CoverartProvider}
import mobi.upod.util.Duration.LongDuration
import android.media.MediaMetadataRetriever
import scala.util.{Success, Try}

final class VideoPlaybackFragment extends PlaybackFragment with FragmentStateHolder {

  import VideoPlaybackFragment._

  private lazy val coverartProvider = inject[CoverartProvider]
  private lazy val storagePreferences = inject[StoragePreferences]

  private lazy val videoContainer = childViewGroup(R.id.videoContainer)
  private lazy val videoView = videoContainer.childAs[TextureView](R.id.videoView)
  private lazy val previewImageView = videoContainer.childImageView(R.id.previewImage)
  
  private lazy val overlayController = createOverlayController

  protected val viewId = R.layout.video_playback
  private val surfaceController = SurfaceController()

  override protected val podcastImageSize: ImageSize = ImageSize.list

  override protected def requestedScreenOrientation =
    if (playbackPreferences.enforceLandscapeVideo) ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

  private def createOverlayController = if (getActivity.getResources.getBoolean(R.bool.fullScreenVideo))
      new FullscreenVideoOverlayController
    else
      new PartScreenVideoOverlayController

  override def onActivityCreated(savedInstanceState: Bundle): Unit = {
    super.onActivityCreated(savedInstanceState)
    overlayController.init()
    videoView.setSurfaceTextureListener(surfaceController)
  }

  override def onEpisodeChanged(episode: Option[EpisodeBaseWithPlaybackInfo]): Unit = {
    super.onEpisodeChanged(episode)

    if (isLandscape) {
      Try(getActivity.setTitle(episode.map(_.title).getOrElse("")))
    }
  }

  override protected def updateActionBarBackground(color: Color): Unit = {
    supportActionBar.setBackgroundDrawable(getResources.getDrawable(R.drawable.dimmed_overlay_top))
    supportActionBar.setElevation(0)
  }

  override protected def updatePlaybackControlBackground(color: Color): Unit =
    overlayController.updatePlaybackControlBackground(color)

  override def onStart(): Unit = {
    super.onStart()
    surfaceController.onStart()
  }

  override def onStop(): Unit = {
    super.onStop()
  }

  override protected def showEpisode(episode: EpisodeListItem): Unit = {
    super.showEpisode(episode)
    showPreviewImageIfApplicable()
  }

  private def initVideoDisplay(): Unit = {
    if (activityIsAlive) {
      val currentSize = VideoSize(videoContainer.getWidth, videoContainer.getHeight)
      val newSize = playbackService.videoSize.forBoundingBox(currentSize)
      val layoutParams = new LayoutParams(newSize.width, newSize.height, Gravity.CENTER)
      videoView.setLayoutParams(layoutParams)
      if (!isRemotePlayback) {
        playbackPanel.setKeepScreenOn(true)
      }
      hideOverlays(false)
      surfaceController.initVideoSurface()
    }
  }

  private def onVideoStopped(): Unit = {
    if (activityIsAlive) {
      playbackPanel.setKeepScreenOn(false)
      showOverlays()
    }
  }

  private def scheduleOverlayHide(): Unit = {
    val handler = playbackPanel.getHandler
    handler.removeCallbacks(DelayedOverlayHider)
    if (!isRemotePlayback) {
      handler.postDelayed(DelayedOverlayHider, HideDelay)
    }
  }

  private def showOverlays(): Unit = {
    overlayController.showOverlays(true)
    if (playbackService.isPlaying) {
      scheduleOverlayHide()
    }
  }

  private def hideOverlays(manual: Boolean): Unit = {
    if (manual || !isRemotePlayback) {
      overlayController.showOverlays(false)
    }
  }

  override def onStopTrackingTouch(seekBar: SeekBar): Unit = {
    super.onStopTrackingTouch(seekBar)
    scheduleOverlayHide()
  }

  override protected def onSeekAction(): Unit = {
    super.onSeekAction()
    if (playbackService.isPlaying) {
      scheduleOverlayHide()
    }
  }

  private def showPreviewImageIfApplicable(): Unit = {

    def loadPreviewImage: Option[Bitmap] = episode flatMap { e =>
      val storageProvider = storagePreferences.storageProvider
      if (e.downloadInfo.fetchedBytes > 0 && storageProvider.readable) {
        val path = e.mediaFile(storageProvider).getAbsolutePath
        try {
          val metadataRetriever = new MediaMetadataRetriever()
          metadataRetriever.setDataSource(path)
          Option(metadataRetriever.getFrameAtTime)
        } catch {
          case ex: Throwable =>
            log.warn(s"failed to load preview image from file $path", ex)
            None
        }
      } else {
        None
      }
    }

    def loadCoverart: Option[Bitmap] = episode flatMap { e =>
      e.podcastInfo.imageUrl.flatMap(coverartProvider.getImageBitmap(_, ImageSize.full))
    }

    if (playbackService.isIdle || isRemotePlayback) {
      AsyncTask.execute {
        Try(loadPreviewImage) match {
          case Success(Some(img)) =>
            Some(img)
          case _ =>
            Try(loadCoverart).getOrElse(None)
        }
      } { image =>
        image match {
          case Some(img) =>
            if (state.started && (playbackService.isIdle || isRemotePlayback)) {
              previewImageView.show(true)
              previewImageView.setImageDrawable(new BitmapDrawable(getActivity.getResources, img))
            } else {
              img.recycle()
            }
          case _ =>
            image.foreach(_.recycle())
            if (state.started) {
              previewImageView.hide
            }
        }
      }
    }
  }

  def hidePreviewImageOnPlaybackIfApplicable(): Unit = {
    if (!isRemotePlayback) {
      previewImageView.hide()
    }
  }

  //
  // playback listener
  // 

  override def onPlaybackStarted(episode: EpisodeBaseWithPlaybackInfo): Unit = {
    hidePreviewImageOnPlaybackIfApplicable()
    initVideoDisplay()
    super.onPlaybackStarted(episode)
  }

  override def onPlaybackPaused(episode: EpisodeBaseWithPlaybackInfo): Unit = {
    hidePreviewImageOnPlaybackIfApplicable()
    onVideoStopped()
    super.onPlaybackPaused(episode)
  }

  override def onPlaybackStopped(): Unit = {
    onVideoStopped()
    showPreviewImageIfApplicable()
    super.onPlaybackStopped()
  }  

  //
  // Surface Controller
  //

  private trait SurfaceController extends SurfaceTextureListener {

    def onStart(): Unit

    def initVideoSurface(): Unit

    def onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) = {}

    def onSurfaceTextureUpdated(surface: SurfaceTexture) = {}
  }

  private object SurfaceController {
    def apply(): SurfaceController = if (ApiLevel >= ApiLevel.JellyBean)
      new JellyBeanSurfaceController
    else
      new LegacySurfaceController
  }

  private final class LegacySurfaceController extends SurfaceController {
    private var requiresSurface = false

    def onStart() = {}

    def initVideoSurface() = {
      if (videoView.isAvailable)
        propagateSurface(videoView.getSurfaceTexture)
      else
        requiresSurface = true
    }

    def onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int): Unit = {
      if (requiresSurface) {
        propagateSurface(surface)
      }
    }

    def onSurfaceTextureDestroyed(surface: SurfaceTexture) = true

    private def propagateSurface(surface: SurfaceTexture): Unit = {
      requiresSurface = false
      playbackService.setSurface(Some(surface))
    }
  }

  private final class JellyBeanSurfaceController extends SurfaceController {
    private var requireSurface = false

    def onStart(): Unit =
      playbackService.surface.foreach(setSurface)

    def initVideoSurface(): Unit = playbackService.surface match {
      case Some(surface) =>
        requireSurface = false
        setSurface(surface)
      case None if videoView.isAvailable =>
        requireSurface = false
        playbackService.setSurface(Some(videoView.getSurfaceTexture))
      case None =>
        requireSurface = true
    }

    def onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int): Unit = {
      if (requireSurface) {
        requireSurface = false
        playbackService.setSurface(Some(surface))
      } else {
        // hm ... should never happen
        playbackService.surface.foreach(setSurface)
      }
    }

    def onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean =
      !playbackService.setCareForSurface(true)

    private def setSurface(surface: SurfaceTexture): Unit = {
      if (videoView.getSurfaceTexture != surface) {
        videoView.setSurfaceTexture(surface)
      }
    }
  }

  //
  // VideoOverlayController
  //

  private object DelayedOverlayHider extends Runnable {
    def run() = {
      if (activityIsAlive && playbackService.isPlaying && !seeking) {
        hideOverlays(false)
      }
    }
  }

  private trait VideoOverlayController {

    def init(): Unit

    def showOverlays(): Unit =
      VideoPlaybackFragment.this.showOverlays()

    def showOverlays(show: Boolean): Unit

    def updatePlaybackControlBackground(color: Int): Unit
  }
  
  private final class PartScreenVideoOverlayController extends VideoOverlayController {

    private var partScreenControlsVisible = true

    def init(): Unit = {
      videoContainer.onClick(togglePartscreenControls())
    }

    private def togglePartscreenControls(): Unit = {
      if (!partScreenControlsVisible)
        showOverlays()
      else
        hideOverlays(true)
    }

    def showOverlays(show: Boolean): Unit = {
      partScreenControlsVisible = show
      if (show)
        getActivity.asInstanceOf[ActionBarActivity].getSupportActionBar.show()
      else
        getActivity.asInstanceOf[ActionBarActivity].getSupportActionBar.hide()
    }

    override def updatePlaybackControlBackground(color: Int): Unit =
      playbackControl.setBackgroundColor(color)
  }
  
  private final class FullscreenVideoOverlayController
    extends VideoOverlayController 
    with View.OnSystemUiVisibilityChangeListener {

    private var lastSystemUiVisibility = 0

    def init(): Unit = {
      playbackPanel.setOnSystemUiVisibilityChangeListener(this)
      showOverlays(true)
      videoContainer.onClick(showOverlays())
      playbackControl.onClick(hideOverlays(true))
    }
  
    def showOverlays(show: Boolean): Unit = {
      val visibilityFlags = if (show)
        0
      else
        View.SYSTEM_UI_FLAG_LOW_PROFILE |
          View.SYSTEM_UI_FLAG_FULLSCREEN |
          View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
  
      val uiVisibility =
        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
          View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
          View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
          visibilityFlags
  
      playbackPanel.setSystemUiVisibility(uiVisibility)
      playbackControl.makeInvisible(!show)
    }
  
    def onSystemUiVisibilityChange(visibility: Int): Unit = {
      // Detect when we go out of nav-hidden mode, to clear our state
      // back to having the full UI chrome up.  Only do this when
      // the state is changing and nav is no longer hidden.
      val diff = lastSystemUiVisibility ^ visibility
      lastSystemUiVisibility = visibility
      if ((diff & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) != 0 && (visibility & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) == 0) {
        VideoPlaybackFragment.this.showOverlays()
      }
    }

    override def updatePlaybackControlBackground(color: Int): Unit = ()
  }
}

object VideoPlaybackFragment {
  val HideDelay = 3.seconds
}