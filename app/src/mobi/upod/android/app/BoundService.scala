package mobi.upod.android.app

import android.app.Service
import android.content.Intent
import android.os.Binder

abstract class BoundService[A] extends Service {
  self: A =>

  private val binder = new ControllerBinding

  def onBind(intent: Intent) = binder

  class ControllerBinding extends Binder {
    def controller: A = BoundService.this
  }
}
