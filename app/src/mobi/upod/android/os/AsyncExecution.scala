package mobi.upod.android.os

trait AsyncExecution extends CreateThreadDetector {

  def async[A](process: => A)(postProcess: A => Unit) = {
    if (isOnCreateThread)
      AsyncTask.execute(process)(postProcess)
    else {
      postProcess(process)
    }
  }

  def async(process: => Unit) = {
    async[Unit](process)(n => n)
  }
}
