package mobi.upod.app.gui.podcast

import android.content.{Context, Intent}
import android.os.Bundle
import android.support.v7.app.ActionBarActivity
import mobi.upod.android.app.UpNavigation
import mobi.upod.android.content.IntentHelpers._
import mobi.upod.app.data.Podcast

class SubscriptionSettingsActivity extends ActionBarActivity with UpNavigation {

  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)
    getFragmentManager.beginTransaction
      .replace(android.R.id.content, new SubscriptionSettingsFragment)
      .commit()
  }

  override protected def navigateUp(): Unit =
    finish()
}

object SubscriptionSettingsActivity {

  def start(context: Context, podcast: Podcast): Unit = {
    val intent = new Intent(context, classOf[SubscriptionSettingsActivity])
    intent.putExtra(FullPodcastSelection, podcast)
    context.startActivity(intent)
  }
}
