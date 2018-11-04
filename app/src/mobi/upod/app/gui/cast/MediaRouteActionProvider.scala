package mobi.upod.app.gui.cast

import android.content.Context
import mobi.upod.app.AppInjection

class MediaRouteActionProvider(context: Context)
  extends android.support.v7.app.MediaRouteActionProvider(context)
  with AppInjection {

  override def onCreateMediaRouteButton(): MediaRouteButton =
    new MediaRouteButton(context)
}
