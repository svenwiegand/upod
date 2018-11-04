package mobi.upod.app.gui.podcast

import android.content.{Context, Intent}
import mobi.upod.android.app.SimpleAlertDialogFragment
import mobi.upod.android.content.IntentHelpers._
import mobi.upod.app.R
import mobi.upod.app.data.{Podcast, PodcastListItem}
import mobi.upod.app.gui.{MainNavigation, PodcastSelection}
import mobi.upod.app.services.OnlinePodcastService

import scala.util.{Failure, Try}

class OpenPodcastUriActivity extends OpenPodcastActivity {

  private lazy val podcastListItem = getIntent.getExtra(PodcastSelection)

  override protected def fetchPodcast: Try[Podcast] = podcastListItem match {
    case Some(p) => inject[OnlinePodcastService].getPodcastDetails(p.uri, p.url)
    case None => Failure(new IllegalArgumentException("podcast item not set"))
  }

  override protected def onPodcastError(error: Throwable): Unit = if (state.created) {
    SimpleAlertDialogFragment.showFromActivity(
      this,
      "alert",
      R.string.open_podcast_url_activity,
      getString(R.string.open_podcast_failed, podcastListItem.map(_.url).getOrElse("?"), error.getLocalizedMessage),
      neutralButtonTextId = Some(R.string.close))
  }

  override protected def navigationItemId = MainNavigation.findPodcasts
}

object OpenPodcastUriActivity {

  def start(context: Context, podcast: PodcastListItem): Unit = {
    val intent = new Intent(context, classOf[OpenPodcastUriActivity])
    intent.putExtra(PodcastSelection, podcast)
    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
    context.startActivity(intent)
  }
}
