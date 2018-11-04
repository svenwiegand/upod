package mobi.upod.app.gui.playback

import android.app.Activity
import android.content.{Context, Intent}
import android.os.Bundle
import android.support.v4.app.NavUtils
import android.support.v7.app.ActionBarActivity
import android.view.Menu
import mobi.upod.android.app.FragmentTransactions.RichFragmentManager
import mobi.upod.android.app.{ActivityStateHolder, ListenerActivity, UpNavigation}
import mobi.upod.android.content.IntentHelpers._
import mobi.upod.app.data.{EpisodeBaseWithPlaybackInfo, EpisodeListItem}
import mobi.upod.app.gui.episode.PlayingEpisodeExtra
import mobi.upod.app.gui.{MainActivity, MainNavigation, MediaRouteUi}
import mobi.upod.app.services.playback.{PlaybackListener, PlaybackService}
import mobi.upod.app.{AppInjection, R}
import mobi.upod.util.ExceptionUtil

final class PlaybackActivity
  extends ActionBarActivity
  with PlaybackListener
  with ListenerActivity
  with ActivityStateHolder
  with MediaRouteUi
  with UpNavigation
  with AppInjection {

  protected def observables = Traversable(inject[PlaybackService])

  private var playbackFragment: Option[PlaybackFragment] = None

  //
  // lifecycle and fragment handling
  //

  override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(null)
    setTitle("")
    setContentView(R.layout.playback)
    onEpisodeChanged(getIntent.getExtra(PlayingEpisodeExtra))
  }

  override def onCreateOptionsMenu(menu: Menu): Boolean = {
    getMenuInflater.inflate(R.menu.playback_activity_actions, menu)
    super.onCreateOptionsMenu(menu)
    true
  }

  private def setPlaybackFragment(fragment: PlaybackFragment): Unit = if (state.started) {
    getFragmentManager.inTransaction { trx =>
      playbackFragment = Some(fragment)
      trx.replace(R.id.playbackFragment, fragment)
    }
  }

  private def showAudioFragment(): Unit =
    setPlaybackFragment(new AudioPlaybackFragment)

  private def showVideoFragment(): Unit =
    setPlaybackFragment(new VideoPlaybackFragment)

  private def showFragmentForEpisode(episode: EpisodeBaseWithPlaybackInfo): Unit = playbackFragment match {
    case (Some(_: AudioPlaybackFragment) | None) if episode.media.mimeType.isVideo =>
      showVideoFragment()
    case (Some(_: VideoPlaybackFragment) | None) if !episode.media.mimeType.isVideo =>
      showAudioFragment()
    case _ =>
      // leave everything as is
  }

  private def showEmptyFragment(): Unit = playbackFragment match {
    case Some(_: AudioPlaybackFragment) =>
      // do nothing
    case _ =>
      showAudioFragment()
  }

  override protected def navigateUp(): Unit =
    NavUtils.navigateUpTo(this, MainActivity.intent(this, MainNavigation.playlist))

  //
  // PlaybackListener
  //

  override def onEpisodeChanged(episode: Option[EpisodeBaseWithPlaybackInfo]): Unit = if (state.created) {
    episode match {
      case Some(e) =>
        ExceptionUtil.tryAndRecover(showFragmentForEpisode(e))(log.error("failed to show empty fragment", _))
      case None =>
        ExceptionUtil.tryAndRecover(showEmptyFragment())(log.error("failed to show empty fragment", _))
    }
  }
}

object PlaybackActivity {

  def intent(context: Context, episode: Option[EpisodeListItem] = None): Intent = {
    val intent = new Intent(context, classOf[PlaybackActivity])
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP)
    episode foreach { e =>
      intent.putExtra(PlayingEpisodeExtra, e)
    }
    intent
  }

  def start(activity: Activity, episode: Option[EpisodeListItem] = None, extras: Option[Bundle] = None): Unit = activity match {
    case _: PlaybackActivity => // already showing playback activity -- avoid additional start
    case _: PlaybackShowNotesActivity => // showing playback show notes on top of a playback activity -- user propably doesn't want to get it dismissed
    case _ => activity.startActivity(intent(activity, episode), extras.orNull)
  }
}
