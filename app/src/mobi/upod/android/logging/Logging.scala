package mobi.upod.android.logging


trait Logging {
  protected lazy val log = new Logger(getClass)
}
