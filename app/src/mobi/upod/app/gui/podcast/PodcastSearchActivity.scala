package mobi.upod.app.gui.podcast

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.ActionBarActivity
import mobi.upod.android.app.StandardUpNavigation
import mobi.upod.app.R

class PodcastSearchActivity extends ActionBarActivity with StandardUpNavigation {

  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.podcast_search)
  }

  override def onNewIntent(intent: Intent): Unit = {
    super.onNewIntent(intent)
    setIntent(intent)
    getFragmentManager.findFragmentById(R.id.podcasts).asInstanceOf[SearchPodcastsGridFragment].reload()
  }
}
