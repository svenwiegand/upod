package mobi.upod.app.services.cast

import mobi.upod.app.services.playback.player.MediaPlayer
import java.net.URL
import mobi.upod.app.services.playback.RemotePlaybackState.RemotePlaybackState
import mobi.upod.app.services.playback.RemotePlaybackState


trait MediaRouteDevice {

  def isInternetStreamingDevice: Boolean

  def currentMediaUrl: Option[URL]

  def currentPlaybackState: RemotePlaybackState.RemotePlaybackState
  
  def shutdown(): Unit

  def mediaPlayer: Option[MediaPlayer]

  def increaseVolume(): Unit

  def decreaseVolume(): Unit
}
