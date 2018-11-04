package mobi.upod.android.content

import android.content.Loader
import mobi.upod.android.util.ApiLevel

trait CompatLoader[D] extends Loader[D] {

  def cancelLoadIfSupported(): Boolean = {
    if (ApiLevel >= ApiLevel.JellyBean)
      cancelLoad() // according to documentation cancelLoad() is already available in honeycomb but in the android sources it occurs starting with JellyBean
    else
      false
  }
}
