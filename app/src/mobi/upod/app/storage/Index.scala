package mobi.upod.app.storage

import mobi.upod.sql.Sql

case class Index(table: Symbol, name: Symbol, unique: Boolean, columns: IndexColumn*) {

  def sql = {
    val uniqueStr = if (unique) "UNIQUE" else ""
    Sql(s"CREATE $uniqueStr INDEX ${name.name} ON ${table.name} (${columns.mkString(", ")})")
  }

  override def toString = sql.toString
}