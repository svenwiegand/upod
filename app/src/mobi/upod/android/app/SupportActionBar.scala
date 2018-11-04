package mobi.upod.android.app

import android.app.Activity
import android.support.v7.app.{ActionBar, ActionBarActivity}

trait SupportActionBar {

  def getActivity: Activity

  def supportActionBar: ActionBar =
    getActivity.asInstanceOf[ActionBarActivity].getSupportActionBar
}
