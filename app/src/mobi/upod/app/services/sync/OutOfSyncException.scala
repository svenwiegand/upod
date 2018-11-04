package mobi.upod.app.services.sync

import mobi.upod.android.app.AppException

class OutOfSyncException(msg: String) extends AppException(message = Some(msg))
