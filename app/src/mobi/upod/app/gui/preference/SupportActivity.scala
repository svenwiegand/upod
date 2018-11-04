package mobi.upod.app.gui.preference

import android.os.Bundle
import android.support.v7.app.ActionBarActivity
import mobi.upod.android.app.UpNavigation

class SupportActivity extends ActionBarActivity with UpNavigation {

  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)
    getFragmentManager.beginTransaction
      .replace(android.R.id.content, new SupportFragment)
      .commit()
  }

  override protected def navigateUp(): Unit =
    finish()
}
