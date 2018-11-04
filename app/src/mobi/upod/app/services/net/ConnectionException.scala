package mobi.upod.app.services.net

import mobi.upod.android.app.AppException

class ConnectionException(cause: Throwable) extends AppException(cause = Option(cause))
