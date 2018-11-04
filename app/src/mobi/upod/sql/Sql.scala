package mobi.upod.sql

class Sql(val sql: String) extends AnyVal {

  override def toString: String = sql
}

object Sql {

  def apply(sql: String) = new Sql(sql)

  implicit def implicitSql2String(sql: Sql) = sql.sql
}
