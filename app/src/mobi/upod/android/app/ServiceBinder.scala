package mobi.upod.android.app

import android.content.{Context, Intent, ServiceConnection}

import scala.reflect.ClassTag

class ServiceBinder[A, B <: BoundService[A]](implicit serviceTag: ClassTag[B]) {

  private def createIntent(context: Context) =
    new Intent(context, serviceTag.runtimeClass)

  def bind(context: Context, connection: ServiceConnection): Unit = {
    val intent = createIntent(context)
    context.startService(intent)
    context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
  }

  def unbind(context: Context, connection: ServiceConnection): Unit = {
    context.unbindService(connection)
    context.stopService(createIntent(context))
  }
}