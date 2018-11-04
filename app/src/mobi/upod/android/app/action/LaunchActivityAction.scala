package mobi.upod.android.app.action

import android.app.Activity
import android.content.{Intent, Context}

class LaunchActivityAction(activityClass: Class[_ <: Activity]) extends Action {

  def onFired(context: Context) {
    context match {
      case activity: Activity =>
        val intent = new Intent(context, activityClass)
        context.startActivity(intent)
      case _ =>
    }
  }
}
