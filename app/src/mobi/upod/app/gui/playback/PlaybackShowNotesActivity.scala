package mobi.upod.app.gui.playback

import android.app.Activity
import android.content.{Context, Intent}
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.support.v7.app.ActionBarActivity
import android.webkit.WebView
import android.widget.RelativeLayout
import mobi.upod.android.app.StandardUpNavigation
import mobi.upod.android.app.action.{Action, ActivityActions}
import mobi.upod.android.content.IntentHelpers.RichIntent
import mobi.upod.android.view.{ChildViews, WindowCompat}
import mobi.upod.android.widget.FloatingActionButton
import mobi.upod.app.data.{EpisodeBaseWithPlaybackInfo, EpisodeListItem}
import mobi.upod.app.gui.episode.{BrowseEpisodeAction, EpisodeDescriptionViewController, PlayingEpisodeExtra}
import mobi.upod.app.{AppInjection, R}

class PlaybackShowNotesActivity
  extends ActionBarActivity
  with ChildViews
  with EpisodeDescriptionViewController
  with ActivityActions
  with PlaybackPanelWithSeekBar
  with StandardUpNavigation
  with AppInjection {

  private lazy val playbackControls = childViewGroup(R.id.playbackControls)
  override protected val optionsMenuResourceId: Int = R.menu.playback_shownotes_actions

  override protected def createActions: Map[Int, Action] = super.createActions ++ Map(
    R.id.action_browse -> new BrowseEpisodeAction(episode)
  )

  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.playback_shownotes)
    createPlaybackPanel()
    alignSeekBar()
    prepareCloseButton()
    onEpisodeChanged(getIntent.getExtra(PlayingEpisodeExtra))
  }

  private def alignSeekBar(): Unit = {
    seekBar.getLayoutParams match {
      case l: RelativeLayout.LayoutParams =>
        val horizontalOffset = seekBar.getPaddingLeft
        val topOffset = - seekBar.getThumb.getIntrinsicHeight / 2 + seekBar.progressTrackHeight / 2
        l.setMargins(-horizontalOffset, topOffset, -horizontalOffset, 0)
        seekBar.getParent.asInstanceOf[RelativeLayout].updateViewLayout(seekBar, l)
      case _ =>
    }
  }

  private def prepareCloseButton(): Unit = {
    getSupportActionBar.setHomeAsUpIndicator(R.drawable.ic_action_dismiss)
    getSupportActionBar.setHomeActionContentDescription(R.string.close)
  }

  override def onStart(): Unit = {
    super.onStart()
    onActivityStart()
  }

  override def onStop(): Unit = {
    onActivityStop()
    super.onStop()
  }

  override def onEpisodeChanged(episode: Option[EpisodeBaseWithPlaybackInfo]): Unit = {
    super.onEpisodeChanged(episode)
    if (episode.isEmpty)
      navigateUp()
  }

  override protected def showEpisode(episode: EpisodeListItem): Unit = {
    updateTitle(episode)
    asyncUpdateDescriptionView(episodeDao.find(episode.id))
    super.showEpisode(episode)
  }

  private def updateTitle(episode: EpisodeListItem): Unit = {
    setTitle(episode.title)
    getSupportActionBar.setSubtitle(episode.podcastInfo.title)
  }

  override protected def tintViews(episode: EpisodeListItem): Unit = {
    super.tintViews(episode)

    val bgColor = episode.extractedOrGeneratedColors.nonLightBackground
    getSupportActionBar.setBackgroundDrawable(new ColorDrawable(bgColor))
    playbackControls.setBackgroundColor(bgColor)

    val bgColorDimmed = bgColor.dimmed
    WindowCompat.setStatusBarColor(getActivity.getWindow, bgColorDimmed)

    val accentColor = episode.extractedOrGeneratedColors.accentForNonLightBackground(theme)
    mediaPlayButton.asInstanceOf[FloatingActionButton].setColor(accentColor)
    mediaPauseButton.asInstanceOf[FloatingActionButton].setColor(accentColor)

    seekBar.setTint(theme.Colors.White)
  }

  override protected def findDescriptionView: WebView =
    childAs[WebView](R.id.description)

  override def getActivity: Activity = this
}

object PlaybackShowNotesActivity {

  def intent(context: Context, episode: Option[EpisodeListItem] = None): Intent = {
    val intent = new Intent(context, classOf[PlaybackShowNotesActivity])
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
    episode foreach { e =>
      intent.putExtra(PlayingEpisodeExtra, e)
    }
    intent
  }

  def start(context: Activity, episode: Option[EpisodeListItem], extras: Option[Bundle] = None): Unit =
    context.startActivity(intent(context, episode), extras.orNull)
}