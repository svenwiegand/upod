package mobi.upod.app.services.download

import java.io.IOException
import mobi.upod.net.{HttpConnectException, HttpStatusException}
import mobi.upod.app.R
import android.content.Context

object DownloadError {
  val statusCodeToStringResource = Map(
    202 -> R.string.http_status_202,
    502 -> R.string.http_status_502,
    405 -> R.string.http_status_405,
    400 -> R.string.http_status_400,
    408 -> R.string.http_status_408,
    409 -> R.string.http_status_409,
    201 -> R.string.http_status_201,
    413 -> R.string.http_status_413,
    403 -> R.string.http_status_403,
    504 -> R.string.http_status_504,
    410 -> R.string.http_status_410,
    500 -> R.string.http_status_500,
    411 -> R.string.http_status_411,
    301 -> R.string.http_status_301,
    302 -> R.string.http_status_302,
    300 -> R.string.http_status_300,
    204 -> R.string.http_status_204,
    406 -> R.string.http_status_406,
    203 -> R.string.http_status_203,
    404 -> R.string.http_status_404,
    501 -> R.string.http_status_501,
    304 -> R.string.http_status_304,
    200 -> R.string.http_status_200,
    206 -> R.string.http_status_206,
    402 -> R.string.http_status_402,
    412 -> R.string.http_status_412,
    407 -> R.string.http_status_407,
    414 -> R.string.http_status_414,
    205 -> R.string.http_status_205,
    303 -> R.string.http_status_303,
    500 -> R.string.http_status_500,
    305 -> R.string.http_status_305,
    401 -> R.string.http_status_401,
    415 -> R.string.http_status_415,
    503 -> R.string.http_status_503,
    505 -> R.string.http_status_505)

  def apply(context: Context, ex: Throwable): String = {
    def HttpError(status: Int): String = statusCodeToStringResource.get(status) match {
      case Some(resId) =>
        context.getString(resId)
      case None =>
        context.getString(R.string.http_status_generic, int2Integer(status))
    }

    ex match {
      case ex: HttpStatusException =>
        HttpError(ex.status)
      case ex @(_: HttpConnectException | _: IOException) =>
        ex.getLocalizedMessage
    }
  }
}
