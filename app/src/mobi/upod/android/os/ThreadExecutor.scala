package mobi.upod.android.os

import android.os.{Handler, Looper}

class ThreadExecutor protected (handler: Handler) {

  def post(execute: => Unit) {
    handler.post(new Runnable {
      def run() {
        execute
      }
    })
  }
}

object ThreadExecutor {
  def apply(handler: Handler) = new ThreadExecutor(handler)

  def apply(looper: Looper) = new ThreadExecutor(new Handler(looper))

  def apply() = new ThreadExecutor(new Handler(Looper.getMainLooper))
}