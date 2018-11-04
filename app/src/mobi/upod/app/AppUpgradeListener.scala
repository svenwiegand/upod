package mobi.upod.app

trait AppUpgradeListener {

  def onAppUpgrade(oldVersion: Int, newVersion: Int): Unit
}
