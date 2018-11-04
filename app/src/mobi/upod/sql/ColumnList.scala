package mobi.upod.sql

case class ColumnList(columns: Symbol*) {
  override def toString = columns map { _.name }  mkString ", "
}
