package mobi.upod.android.content

import android.content.{Context, AsyncTaskLoader}
import mobi.upod.io._
import mobi.upod.util.Cursor

class AsyncCursorLoader[A](context: Context, loadCursor: => Cursor[A], val name: String = "")
  extends AsyncTaskLoader[IndexedSeq[A]](context)
  with CompatLoader[IndexedSeq[A]] {

  private var data: Option[IndexedSeq[A]] = None

  def loadInBackground() = {
    forCloseable (loadCursor) { cursor =>
      cursor.toIndexedSeq
    }
  }

  override def onStartLoading() {
    if (takeContentChanged || data.isEmpty)
      forceLoad()
    else if (data.isDefined)
      deliverResult(data.get)
  }

  override def deliverResult(data: IndexedSeq[A]) {
    if (!isReset) {
      this.data = Some(data)
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

object AsyncCursorLoader {
  def apply[A](context: Context, loadCursor: => Cursor[A], name: String = "") =
    new AsyncCursorLoader(context, loadCursor, name)
}
