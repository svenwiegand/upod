package mobi.upod.app.services.playback

import mobi.upod.android.content.{RemoteActionIntent, RemoteActionReceiver}
import mobi.upod.app.{R, AppInjection}

class RemotePlaybackActionReceiver extends RemoteActionReceiver with AppInjection {

  val intentBuilder = RemotePlaybackActionReceiver

  protected def createActions = Map(
    R.id.action_media_stop -> new StopPlaybackAction(),
    R.id.action_media_pause -> new PausePlaybackAction(),
    R.id.action_media_resume -> new ResumePlaybackAction(true),
    R.id.action_media_skip -> new SkipEpisodePlaybackAction(),
    R.id.action_media_fast_forward -> new FastForwardPlaybackAction(),
    R.id.action_media_rewind -> new RewindPlaybackAction()
  )
}

object RemotePlaybackActionReceiver extends RemoteActionIntent("REMOTE_PLAYBACK_ACTION")