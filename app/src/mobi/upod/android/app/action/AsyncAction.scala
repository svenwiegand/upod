package mobi.upod.android.app.action

import mobi.upod.android.os.AsyncTask
import android.content.Context

abstract class AsyncAction[A, B] extends Action with AsyncActionHook {

  final def onFired(context: Context) {
    preProcess(context)
    val data = getData(context)
    if (shouldExecute(context, data)) {
      preProcessData(context, data)
      AsyncTask.execute(processData(context, data)) {
        result =>
          postProcessData(context, result)
          postProcess(context)
      }
    }
  }

  protected def getData(context: Context): A

  protected def shouldExecute(context: Context, data: A): Boolean = true

  protected def preProcessData(context: Context, data: A) {
    // do nothing by default
  }

  protected def processData(context: Context, data: A): B

  protected def postProcessData(context: Context, result: B) {
    // do nothing by default
  }
}
