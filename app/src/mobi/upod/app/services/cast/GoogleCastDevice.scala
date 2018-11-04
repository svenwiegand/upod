package mobi.upod.app.services.cast

import android.content.Context
import android.os.Bundle
import com.escalatesoft.subcut.inject.{Injectable, BindingModule}
import com.google.android.gms.cast.Cast.ApplicationConnectionResult
import com.google.android.gms.cast._
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.{ResultCallback, PendingResult, GoogleApiClient}
import java.io.IOException
import java.net.URL
import mobi.upod.android.logging.Logging
import mobi.upod.app.services.playback.player.MediaPlayer
import mobi.upod.app.storage.InternalAppPreferences
import mobi.upod.app.services.playback.RemotePlaybackState.RemotePlaybackState

class GoogleCastDevice(
    context: Context,
    device: CastDevice,
    requireExistingSession: Boolean,
    requestDisconnect: Boolean => Unit,
    onConnected: Option[GoogleCastDevice] => Unit)(
    implicit val bindingModule: BindingModule)
  extends Cast.Listener
  with MediaRouteDevice
  with GoogleApiClient.ConnectionCallbacks
  with GoogleApiClient.OnConnectionFailedListener
  with Injectable
  with Logging {

  import GoogleCastDevice._

  private val castSessionIdPreference = inject[InternalAppPreferences].castSessionId

  private val apiClient: GoogleApiClient = {
    val optionsBuilder = Cast.CastOptions.builder(device, this)
    new GoogleApiClient.Builder(context).
      addApi(Cast.API, optionsBuilder.build).
      addConnectionCallbacks(this).
      addOnConnectionFailedListener(this).
      build
  }

  private lazy val _mediaPlayer = new GoogleCastMediaPlayer(new RemoteMediaPlayer, apiClient, castApi)
  override lazy val mediaPlayer: Option[MediaPlayer] = Some(_mediaPlayer)

  log.info(s"launching cast receiver app on ${device.getFriendlyName}")
  apiClient.connect()

  override val isInternetStreamingDevice = true

  override def currentMediaUrl: Option[URL] =
    _mediaPlayer.currentMediaUrl

  override def currentPlaybackState: RemotePlaybackState =
    _mediaPlayer.currentPlaybackState

  private def adjustVolume(increment: Double): Unit = {
    val currentVolume = castApi.getVolume(apiClient)
    val volume = math.max(math.min(currentVolume + increment, 1.0), 0.0)
    try {
      castApi.setVolume(apiClient, volume)
    } catch {
      case ex: Exception =>
        log.error(s"failed to set volume $volume", ex)
    }
  }

  override def decreaseVolume(): Unit =
    adjustVolume(-VolumeIncrement)

  override def increaseVolume(): Unit =
    adjustVolume(VolumeIncrement)

  private def connectPlayer(): Unit = {
    try {
      _mediaPlayer.connect { result =>
        val status = result.getStatus
        if (status.isSuccess) {
          log.info("successfully retrieved status")
          onConnected(Some(GoogleCastDevice.this))
        } else {
          log.error(s"failed to request status: ${status.getStatusCode}")
          onConnected(None)
          disconnect(false)
        }
      }
    } catch {
      case ex: IOException =>
        log.error("failed to create media channel", ex)
        onConnected(None)
        disconnect(false)
    }
  }

  private def disconnectPlayer(): Unit =
    tryOrLog(_mediaPlayer.disconnect())

  def shutdown(): Unit = {
    log.info("shutting down cast client")
    disconnectPlayer()
    tryOrLog(castApi.stopApplication(apiClient))
    tryOrLog(apiClient.disconnect())
  }

  private def disconnect(reconnectWhenPossible: Boolean): Unit =
    requestDisconnect(reconnectWhenPossible)

  //
  // connection listeners
  //

  override def onConnected(connectionHint: Bundle): Unit = {
    try {
      val result: PendingResult[Cast.ApplicationConnectionResult] = castSessionIdPreference.option match {
        case Some(sessionId) if requireExistingSession =>
          log.info(s"trying to join running app with session id $sessionId")
          castApi.joinApplication(apiClient, AppId, sessionId)
        case _ =>
          log.info("launching app")
          castApi.launchApplication(apiClient, AppId, false)
      }
      result.setResultCallback(new ResultCallback[ApplicationConnectionResult] {
        override def onResult(result: ApplicationConnectionResult): Unit = {
          val status = result.getStatus
          if (status.isSuccess) {
            log.info("successfully joined/launched receiver app")
            castSessionIdPreference := result.getSessionId
            connectPlayer()
          } else {
            log.error(s"failed to launch receiver app with status $status")
            castSessionIdPreference := None
            onConnected(None)
            disconnect(false)
          }
        }
      })
    } catch {
      case ex: Exception =>
        log.error("failed to launch receiver app", ex)
    }
  }

  override def onConnectionSuspended(cause: Int): Unit = {
    log.info(s"remote connection suspended with cause $cause")
    disconnect(true)
  }

  override def onConnectionFailed(result: ConnectionResult): Unit = {
    log.error(s"connection failed with result $result")
    disconnect(false)
  }

  override def onApplicationStatusChanged(): Unit = {
    if (apiClient.isConnected) {
      log.info(s"receiver application status changed to ${castApi.getApplicationStatus(apiClient)}")
    }
  }

  override def onApplicationDisconnected(statusCode: Int): Unit = {
    disconnect(false)
  }

  private def tryOrLog(operation: => Unit): Unit = {
    try {
      operation
    } catch {
      case ex: Throwable =>
        log.error("operation failed", ex)
    }
  }
}

object GoogleCastDevice {
  val AppId = "AC1A8493"
  val CategoryGoogleCast = CastMediaControlIntent.categoryForCast(AppId)
  val VolumeIncrement = 0.05

  type CastApi = Cast.CastApi
  private def castApi: CastApi = Cast.CastApi
}