package mobi.upod.app.gui

import android.app.Activity
import android.support.v7.app.ActionBarActivity
import mobi.upod.android.app.action.{Action, ActivityActions}
import mobi.upod.app.R
import mobi.upod.app.gui.playback.PlaybackPanel
import mobi.upod.app.services.licensing.OpenGooglePlayLicenseAction

trait ActivityWithPlaybackPanelAndStandardActions
  extends ActionBarActivity
  with PlaybackPanel
  with ActivityActions
  with MediaRouteUi {

  protected val optionsMenuResourceId = R.menu.standard_activity_actions

  override protected def createActions: Map[Int, Action] = super.createActions ++ Map(
    R.id.action_purchase -> new OpenGooglePlayLicenseAction
  )

  def getActivity: Activity = this

  override def onStart(): Unit = {
    super.onStart()
    getSupportActionBar.setElevation(0f)
    onActivityStart()
  }

  override def onStop(): Unit = {
    onActivityStop()
    super.onStop()
  }

  override protected def showControls(show: Boolean): Unit = {
    super.showControls(show)
    playbackPanel.show(show)
  }
}
