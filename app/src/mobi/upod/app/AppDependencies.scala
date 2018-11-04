package mobi.upod.app

import com.escalatesoft.subcut.inject.BindingModule

trait AppDependencies extends BindingModule {

  def onAppCreate(): Unit

  def onAppTerminate(): Unit

  def onUpgrade(oldVersion: Int, newVersion: Int): Unit
}
