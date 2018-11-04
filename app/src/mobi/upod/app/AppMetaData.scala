package mobi.upod.app

import android.os.Bundle

class AppMetaData(metaData: Bundle) {
  val developmentMode = metaData.getBoolean("developmentMode")
  val upodServiceUrl = metaData.getString("upodServiceUrl")
}
