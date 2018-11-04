package mobi.upod.app.services.cast

import com.google.android.gms.common.api.{Result, Status, ResultCallback, PendingResult}
import com.google.android.gms.cast.RemoteMediaPlayer

private[cast] object GoogleCastUtils {

  final implicit class RichPendingMediaChannelResult(val result: PendingResult[RemoteMediaPlayer.MediaChannelResult]) extends AnyVal {

    def onResult(handleResult: RemoteMediaPlayer.MediaChannelResult => Unit): Unit =
      result.setResultCallback(ResultCallback(handleResult))

    def fold(onError: Status => Unit, onSuccess: Status => Unit): Unit = onResult { result =>
      val status = result.getStatus
      if (status.isSuccess)
        onSuccess(status)
      else
        onError(status)
    }

    def onError(handleError: Status => Unit): Unit = fold(handleError, _ => ())

    def onSuccess(handleSuccess: Status => Unit): Unit = fold(_ => (), handleSuccess)
  }

  def ResultCallback[A <: Result](handleResult: A => Unit): ResultCallback[A] = new ResultCallback[A] {
    override def onResult(result: A): Unit =
      handleResult(result)
  }
}
