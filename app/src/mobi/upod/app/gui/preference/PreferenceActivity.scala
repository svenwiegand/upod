package mobi.upod.app.gui.preference

import java.util

import android.os.Bundle
import android.preference.PreferenceActivity.Header
import android.support.v4.app.NavUtils
import android.view.MenuItem
import mobi.upod.android.app.AppCompatPreferenceActivity
import mobi.upod.android.app.permission.PermissionRequestingActivity
import mobi.upod.app.{AppInjection, R}

class PreferenceActivity extends AppCompatPreferenceActivity
  with PermissionRequestingActivity
  with StoragePermissionRequestActivity
  with AppInjection {

  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)
    getSupportActionBar.setDisplayHomeAsUpEnabled(true)
  }

  override def onBuildHeaders(target: util.List[Header]) {
    loadHeadersFromResource(R.xml.pref_headers, target)
  }

  override def isValidFragment(fragmentName: String): Boolean = true

  override def onOptionsItemSelected(item: MenuItem): Boolean = {
    item.getItemId match {
      case android.R.id.home =>
        navigateUp()
        true
      case _ =>
        super.onOptionsItemSelected(item)
    }
  }

  private def navigateUp(): Unit =
    NavUtils.navigateUpFromSameTask(this)
}

