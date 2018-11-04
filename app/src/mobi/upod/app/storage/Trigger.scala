package mobi.upod.app.storage

import mobi.upod.sql.Implicits._
import mobi.upod.sql.Sql

case class Trigger(name: Symbol, definition: Sql, statements: Sql*) {

  def createSql: Sql = {
    val block = statements.mkString("\n  ", ";\n", ";\n")
    Sql(s"CREATE TRIGGER ${name.name} $definition BEGIN $block END")
  }

  def dropSql: Sql = sql"DROP TRIGGER IF EXISTS $name"
}


