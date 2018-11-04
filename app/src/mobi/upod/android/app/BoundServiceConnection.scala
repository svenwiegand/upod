package mobi.upod.android.app

import android.content.{ComponentName, Context, ServiceConnection}
import android.os.IBinder
import mobi.upod.android.logging.Logging

import scala.collection.mutable

trait BoundServiceConnection[A] extends ServiceConnection {
  self: Logging =>

  protected def serviceContext: Context
  protected def serviceBinder: ServiceBinder[A, _ <: BoundService[A]]
  private var _serviceController: Option[A] = None
  private val pendingCalls = mutable.Queue[A => Unit]()

  final protected def serviceController = _serviceController

  protected def bindService() {
    serviceBinder.bind(serviceContext, this)
  }

  protected def unbindService() {
    if (isServiceConnected) {
      serviceBinder.unbind(serviceContext, this)
      onServiceDisconnected()
    }
  }

  protected def callService(action: A => Unit) {
    _serviceController match {
      case Some(controller) => action(controller)
      case None =>
        pendingCalls.enqueue(action)
        bindService()
    }
  }

  protected def callServiceIfBound[B](action: A => B): Option[B] =
    _serviceController.map(action)

  protected def isServiceConnected = _serviceController.isDefined

  protected def onServiceConnected(controller: A) {
    pendingCalls.dequeueAll(_ => true).foreach(_(controller))
  }

  final def onServiceConnected(name: ComponentName, service: IBinder) {
    log.info(s"$name connected")
    val controller = service.asInstanceOf[BoundService[A]#ControllerBinding].controller
    _serviceController = Some(controller)
    onServiceConnected(controller)
  }

  final def onServiceDisconnected(name: ComponentName) {
    onServiceDisconnected()
  }

  protected def onServiceDisconnected() {
    log.info(s"service disconnected")
    _serviceController = None
  }
}
