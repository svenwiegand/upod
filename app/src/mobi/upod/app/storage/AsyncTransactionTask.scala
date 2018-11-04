package mobi.upod.app.storage

import com.escalatesoft.subcut.inject.BindingModule
import mobi.upod.android.os.AsyncTask

object AsyncTransactionTask {

  def execute(process: => Unit)(implicit bindings: BindingModule) = {
    val db = bindings.inject[Database](None)
    AsyncTask.execute {
      db.newTransaction(process)
    }
  }
}
