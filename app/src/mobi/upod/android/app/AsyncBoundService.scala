package mobi.upod.android.app

import java.util.concurrent.{ExecutorService, Executors, Future}

import android.content.Intent
import mobi.upod.util.concurrent.CancelableRunnable

import scala.collection.mutable

class AsyncBoundService[A] extends BoundService[A] { self: A =>
  import mobi.upod.android.app.AsyncBoundService._

  private var executor: Option[ExecutorService] = None
  private val tasks = new mutable.SynchronizedQueue[Task]()

  private def createExecutor: ExecutorService =
    Executors.newSingleThreadExecutor()

  private def ensureExecutorCreated(): Unit = this.synchronized {
    if (executor.isEmpty) {
      executor = Some(createExecutor)
    }
  }

  private def ensureExecutorDestroyed(): Unit = this.synchronized {
    if (executor.nonEmpty) {
      executor.foreach(_.shutdown())
      executor = None
    }
  }

  def cancelCurrentTasks(): Unit = {
    tasks.dequeueAll(t => t.done || t.cancelled)
    tasks.dequeueAll(_.cancel())
  }

  def cancelCurrentTaskIf(condition: CancelableRunnable => Boolean): Unit = {
    tasks.dequeueFirst(task => condition(task.runnable)) foreach { task =>
      task.cancel()
    }
  }

  def currentTask: Option[CancelableRunnable] =
    tasks.headOption.map(_.runnable)

  private def trackTask(runnable: CancelableRunnable, future: Future[_]): Task = {
    val task = new Task(runnable, future)
    tasks.enqueue(task)
    task
  }

  final protected def execute(task: CancelableRunnable): Task = executor match {
    case Some(exec) =>
      cancelCurrentTasks()
      trackTask(task, exec.submit(task))
    case None => throw new IllegalStateException("executor not running")
  }

  final protected def shutdownNow() {
    cancelCurrentTasks()
    executor.foreach(_.shutdownNow())
    executor = None
  }

  override def onStartCommand(intent: Intent, flags: Int, startId: Int): Int = {
    ensureExecutorCreated()
    super.onStartCommand(intent, flags, startId)
  }

  override def onBind(intent: Intent) = {
    ensureExecutorCreated()
    super.onBind(intent)
  }

  override def onUnbind(intent: Intent) = {
    ensureExecutorDestroyed()
    super.onUnbind(intent)
  }

  override def onDestroy(): Unit = {
    ensureExecutorDestroyed()
    super.onDestroy()
  }
}

object AsyncBoundService {

  final class Task(val runnable: CancelableRunnable, val future: Future[_]) {

    def cancel(): Boolean = {
      runnable.cancel()
      future.cancel(true)
    }

    def cancelled: Boolean = future.isCancelled

    def done: Boolean = future.isDone
  }
}