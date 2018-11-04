package mobi.upod.app.gui

import mobi.upod.app.{IntentExtraKey}
import android.content.Intent
import mobi.upod.android.os.BundleParcelableValue

object ParentActivityIntent extends BundleParcelableValue[Intent](IntentExtraKey("parentActivityIntent"))
