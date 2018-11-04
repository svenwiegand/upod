package mobi.upod.app.storage

import mobi.upod.sql.Implicits._
import mobi.upod.sql.Sql

sealed trait IndexColumn {
  val name: Symbol
  val ascending: Boolean

  protected def order = if (ascending) sql"ASC" else sql"DESC"

  def sql: Sql

  override def toString = sql.toString
}

case class IntIndexColumn(name: Symbol, ascending: Boolean = true) extends IndexColumn {

  def sql = {
    sql"$name $order"
  }
}

case class TextIndexColumn(name: Symbol, ascending: Boolean = true, caseInsensitive: Boolean = true)
  extends IndexColumn {

  def sql = {
    val collation = if (caseInsensitive) sql"COLLATE NOCASE" else sql""
    sql"$name $collation $order"
  }
}
