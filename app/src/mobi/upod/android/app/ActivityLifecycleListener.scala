package mobi.upod.android.app

trait ActivityLifecycleListener {

  def onActivityCreated(): Unit = ()

  def onActivityStart(): Unit = ()

  def onActivityResume(): Unit = ()

  def onActivityPause(): Unit = ()

  def onActivityStop(): Unit = ()

  def onActivityDestroyed(): Unit = ()
}
