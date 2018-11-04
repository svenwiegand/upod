package mobi.upod.app.storage

trait DaoObject {
  val table: Symbol

  protected def Table(name: Symbol) = name

  protected def Column(name: Symbol) = name
}
