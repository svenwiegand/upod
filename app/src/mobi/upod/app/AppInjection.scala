package mobi.upod.app

import com.escalatesoft.subcut.inject.Injectable

trait AppInjection extends Injectable {
  implicit lazy val app = App.instance
  implicit lazy val bindingModule = app.bindingModule
}
