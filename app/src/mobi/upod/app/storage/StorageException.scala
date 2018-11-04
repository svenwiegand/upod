package mobi.upod.app.storage

import java.io.IOException

class StorageException(message: String) extends IOException(message)

class StorageNotReadableException(message: String) extends StorageException(message)
class StorageNotWritableException(message: String) extends StorageException(message)