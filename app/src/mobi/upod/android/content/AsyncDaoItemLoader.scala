package mobi.upod.android.content

import android.content.{AsyncTaskLoader, Context}

class AsyncDaoItemLoader[A](context: Context, loadItem: => Option[A])
  extends AsyncTaskLoader[Option[A]](context)
  with CompatLoader[Option[A]] {

  private var data: Option[A] = None

  def loadInBackground() = loadItem

  override def onStartLoading() {
    if (takeContentChanged || data.isEmpty)
      forceLoad()
    else if (data.isDefined)
      deliverResult(data)
  }

  override def deliverResult(data: Option[A]) {
    if (!isReset) {
      this.data = data
      super.deliverResult(data)
    }
  }

  override def onStopLoading() {
    cancelLoadIfSupported()
  }

  override def onReset() {
    onStopLoading()
    data = None
  }
}

object AsyncDaoItemLoader {
  def apply[A](context: Context, loadItem: => Option[A]) = new AsyncDaoItemLoader(context, loadItem)
}
