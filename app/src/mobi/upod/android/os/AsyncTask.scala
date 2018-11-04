package mobi.upod.android.os

import android.content.Context
import mobi.upod.android.app.WaitDialogFragment
import mobi.upod.android.logging.Logging

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future, ExecutionContext}

object AsyncTask extends Logging {
  def apply[A](process: => A)(postProcess: A => Unit) = new android.os.AsyncTask[AnyRef, Integer, A] {
    def doInBackground(params: AnyRef*): A = {
      log.debug(s"async task $this up and running")
      val result = process
      log.debug(s"async task $this done")
      result
    }

    override def onPostExecute(result: A): Unit = {
      log.debug(s"post processing results of async task $this")
      postProcess(result)
    }
  }

  def execute[A](process: => A)(postProcess: A => Unit): android.os.AsyncTask[AnyRef, Integer, A] = {
    val task = apply(process)(postProcess).executeOnExecutor(android.os.AsyncTask.THREAD_POOL_EXECUTOR)
    log.debug(s"scheduled async task $task")
    task
  }

  def execute(process: => Unit): android.os.AsyncTask[AnyRef, Integer, Unit] = {
    execute[Unit](process)(n => n)
  }

  def executeWithWaitDialog(context: Context, waitMsgId: Int)(process: => Unit): android.os.AsyncTask[AnyRef, Integer, Unit] = {
    WaitDialogFragment.show(context, waitMsgId)
    execute[Unit](process) { _ =>
      WaitDialogFragment.dismiss(context)
    }
  }

  /** Creates a `Future` that runs on the app's async task thread pool.
    *
    * @param process the task to be executed asynchronously
    * @tparam A result type of the asynchronous task
    * @return the future
    */
  def future[A](process: => A): Future[A] =
    Future(process)(AsyncTaskExecutionContext)

  /** Waits forever that the result of the specified future becomes available.
    *
    * Don't use this from the UI thread as it might block.
    *
    * @param future the future to wait for to finish
    * @tparam A type of the future's result
    * @return the future's result after it finished
    */
  def awaitResult[A](future: Future[A]): A =
    Await.result(future, Duration.Inf)

  /** Asynchronously waits for the specified future to finish and processes it's result on the UI thread.
    *
    * If the future has already finished when this method is called, the `postProcess` is executed immediately on the
    * calling thread. Otherwise an `AsyncTask` is launched which waits for the future to complete and post processes the
    * result on the UI thread afterwards.
    *
    * @param future the future to process the result for
    * @param postProcess the post processing operation to be executed on the future's result on the UI thread
    * @tparam A type of the future's result
    */
  def onResult[A](future: Future[A])(postProcess: A => Unit): Unit = {
    if (future.isCompleted)
      postProcess(awaitResult(future))
    else
      execute(awaitResult(future))(postProcess)
  }

  //
  // ExecutionContext implementation
  //

  implicit object AsyncTaskExecutionContext extends ExecutionContext {

    override def execute(runnable: Runnable): Unit =
      android.os.AsyncTask.THREAD_POOL_EXECUTOR.execute(runnable)

    override def reportFailure(cause: Throwable): Unit =
      log.crashLogError("async execution failed", cause)
  }
}
