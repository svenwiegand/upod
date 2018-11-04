package mobi.upod.app.storage

object StorageState extends Enumeration {
  type StorageState = Value
  val NotAvailable = Value("NotAvailable")
  val Readable = Value("Readable")
  val Writable = Value("Writable")
}
