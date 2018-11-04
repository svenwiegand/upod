package mobi.upod.android.app

import android.support.v4.app.NavUtils

trait StandardUpNavigation extends UpNavigation {

  protected def navigateUp() {
    NavUtils.navigateUpFromSameTask(this)
  }
}
