package mobi.upod.app.gui.podcast

import android.content.{Context, Intent}
import android.net.Uri
import java.net.{MalformedURLException, URL}
import mobi.upod.android.app.action.BrowseAction
import mobi.upod.android.app.{AlertDialogListener, SimpleAlertDialogFragment}
import mobi.upod.app.R
import mobi.upod.app.services.OnlinePodcastService
import mobi.upod.app.gui.MainNavigation
import mobi.upod.app.data.Podcast
import scala.util.{Failure, Success, Try}

class OpenPodcastUrlActivity extends OpenPodcastActivity with AlertDialogListener {

  private lazy val podcastUrl: String = {
    def uriFromIntentData: String = Option(getIntent.getData) match {
      case Some(u) => u.toString
      case None => throw new IllegalArgumentException("No URL found by Intent.getData")
    }

    def uriFromStringExtra: String = Option(getIntent.getStringExtra(Intent.EXTRA_TEXT)) match {
      case Some(text) => text
      case None => throw new IllegalArgumentException("No text found in EXTRA_TEXT")
    }

    val uri = getIntent.getAction match {
      case Intent.ACTION_VIEW => uriFromIntentData
      case Intent.ACTION_SEND => uriFromStringExtra
      case _ => throw new IllegalArgumentException("Intent action must be either ACTION_VIEW or ACTION_SEND")
    }

    uri match {
      case url if url.startsWith("http:") || url.startsWith("https:") =>
        url
      case OpenPodcastUrlActivity.AppUriWithSchemePattern(url) =>
        url
      case OpenPodcastUrlActivity.AppUriWithoutSchemePattern(url) =>
        s"http://$url"
      case _ =>
        throw new IllegalArgumentException("Unsupported URI format")
    }
  }

  override protected def validateIntentData(): Boolean = {
    try {
      podcastUrl
      true
    } catch {
      case ex @ (_: IllegalArgumentException | _: MalformedURLException) =>
        log.warn("Failed to extract podcast URL from intent", ex)
        showInvalidUrlDialg()
        false
    }
  }

  override protected def fetchPodcast: Try[Podcast] = {
    val onlinePodcastService = inject[OnlinePodcastService]
    Try(new URL(podcastUrl)) match {
      case Success(u) =>
        onlinePodcastService.getPodcastDetails(u)
      case Failure(ex) =>
        log.warn(s"Invalid URL '$podcastUrl'", ex)
        Failure(ex)
    }
  }

  override protected def onPodcastError(error: Throwable): Unit = if (state.created) {
    SimpleAlertDialogFragment.showFromActivity(
      this,
      "alert",
      R.string.open_podcast_url_activity,
      getString(R.string.open_podcast_url_failed, podcastUrl, error.getLocalizedMessage),
      neutralButtonTextId = Some(R.string.close),
      positiveButtonTextId = Some(R.string.open_podcast_url_in_browser),
      positiveAction = Some(new BrowseAction(podcastUrl)))
  }

  override protected def navigationItemId = MainNavigation.podcasts

  private def showInvalidUrlDialg(): Unit = if (state.created) {
    SimpleAlertDialogFragment.showFromActivity(
      this, "alert", R.string.open_podcast_url_activity, getString(R.string.open_podcast_url_invalid),
      neutralButtonTextId = Some(R.string.close))
  }

  override def onAlertDialogDismissed(dialogTag: String) {
    finish()
  }
}

object OpenPodcastUrlActivity {
  private val AppUriWithSchemePattern = "(?:feed|pcast|podcast|upod|itpc|rss):(?://)?(https?:.*)".r
  private val AppUriWithoutSchemePattern = "(?:feed|pcast|podcast|upod|itpc|rss):(?://)?(.*)".r

  def start(context: Context, url: String): Unit = {
    val intent = new Intent(context, classOf[OpenPodcastUrlActivity])
    intent.setAction(Intent.ACTION_VIEW)
    intent.setData(Uri.parse(url))
    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
    context.startActivity(intent)
  }
}
