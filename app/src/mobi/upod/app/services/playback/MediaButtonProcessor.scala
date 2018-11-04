package mobi.upod.app.services.playback

import android.content.Intent
import android.view.KeyEvent
import android.view.KeyEvent._
import com.escalatesoft.subcut.inject.Injectable
import mobi.upod.android.logging.Logging
import mobi.upod.app.services.licensing.LicenseService
import mobi.upod.app.storage.PlaybackPreferences

trait MediaButtonProcessor extends Injectable with Logging {
  import MediaButtonProcessor._

  private lazy val playbackService = inject[PlaybackService]
  private lazy val playbackPreferences = inject[PlaybackPreferences]
  private lazy val licenseService = inject[LicenseService]

  protected def processMediaButtonEvent(intent: Intent): Boolean = {
    Option(intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT).asInstanceOf[KeyEvent]) exists { event =>
      log.info(s"received media button event ${event.getAction} with keyCode ${event.getKeyCode} (long press: ${event.isLongPress}}, repeat Count: ${event.getRepeatCount}})")
      event.getAction match {
        case ACTION_UP =>
          handleMediaButton(translateMediaButton(event.getKeyCode))
        case _ =>
          false
      }
    }
  }

  private def preferChapterNavigation: Boolean = playbackPreferences.skipBackChapter && licenseService.isLicensed

  private def translateMediaButton(keyCode: Int): Int = {
    if (!playbackPreferences.swapJumpWindButtons) keyCode else keyCode match {
      case KEYCODE_MEDIA_NEXT => KEYCODE_MEDIA_FAST_FORWARD
      case KEYCODE_MEDIA_PREVIOUS => KEYCODE_MEDIA_REWIND
      case KEYCODE_MEDIA_FAST_FORWARD => KEYCODE_MEDIA_NEXT
      case KEYCODE_MEDIA_REWIND => KEYCODE_MEDIA_PREVIOUS
      case code => code
    }
  }

  private def handleMediaButton(keyCode: Int): Boolean = {

    def seekNext(): Unit = {
      if (preferChapterNavigation)
        playbackService.skipChapter()
      else
        playbackService.skip()
    }

    def seekPrevious(): Unit = {
      if (preferChapterNavigation)
        playbackService.goBackChapter()
      else
        playbackService.seek(1)
    }

    val isDoubleClick: Boolean = {
      val currentTime = System.currentTimeMillis
      if (lastKeyPressed == keyCode && (currentTime - lastDownTime) < DoubleClickDelay) {
        lastDownTime = 0
        true
      } else {
        lastDownTime = currentTime
        false
      }
    }

    val processed = keyCode match {
      case KEYCODE_MEDIA_PLAY_PAUSE | KEYCODE_HEADSETHOOK =>
        if (playbackService.isPlaying)
          playbackService.pause()
        else if (playbackService.isPaused)
          playbackService.resume()
        true
      case KEYCODE_MEDIA_PLAY =>
        playbackService.resume()
        true
      case KEYCODE_MEDIA_PAUSE =>
        playbackService.pause()
        true
      case KEYCODE_MEDIA_STOP =>
        playbackService.stop()
        true
      case KEYCODE_MEDIA_NEXT =>
        seekNext()
        true
      case KEYCODE_MEDIA_FAST_FORWARD if isDoubleClick && playbackPreferences.doubleClickToSkipBack =>
        seekNext()
        true
      case KEYCODE_MEDIA_FAST_FORWARD =>
        playbackService.fastForward()
        true
      case KEYCODE_MEDIA_PREVIOUS =>
        seekPrevious()
        true
      case KEYCODE_MEDIA_REWIND if isDoubleClick && playbackPreferences.doubleClickToSkipBack =>
        seekPrevious()
        true
      case KEYCODE_MEDIA_REWIND =>
        playbackService.rewind()
        true
      case _ =>
        false
    }
    lastKeyPressed = keyCode
    processed
  }
}


object MediaButtonProcessor {
  private val DoubleClickDelay = 1500
  private var lastKeyPressed = 0
  private var lastDownTime = 0l
}