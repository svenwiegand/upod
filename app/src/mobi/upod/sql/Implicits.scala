package mobi.upod.sql

import org.joda.time.DateTime

object Implicits {
  private val Escape = 27.toChar.toString

  implicit class SqlValueString(val str: String) extends AnyVal {

    def escapeJsonArray =
      str.replace("'", "''")

    def escape = {
      val escaped = str.replace("'", "''")
      // prevent string starting with [ to be interpreted as JSON array
      if (escaped.startsWith("[")) Escape + escaped else escaped
    }

    def unescape = {
      val unescapePattern = "''"
      val unescaped = str.replace(unescapePattern, "'")
      if (unescaped.startsWith(s"$Escape[")) unescaped.tail else unescaped
    }
  }

  implicit class SqlStringInterpolator(val statement: StringContext) extends AnyVal {

    def sql(arguments: Any*): Sql = {

      def valueToSqlString(value: Any): String = value match {
        case null => "NULL"
        case flag: Boolean => if (flag) "1" else "0"
        case sql: Sql => sql.sql
        case symbol: Symbol => symbol.name
        case columns: ColumnList => columns.toString
        case num @ (_: Int | _: Long | _: Double) => num.toString
        case timestamp: DateTime => timestamp.getMillis.toString
        case option: Option[_] => option.map(valueToSqlString).getOrElse("NULL")
        case seq: Traversable[_] => seq.map(valueToSqlString).mkString(", ")
        case obj => s"'${obj.toString.escape}'"
      }

      val parts = statement.parts.iterator
      val args = arguments.iterator
      val buf = new StringBuilder(parts.next())
      while (parts.hasNext) {
        buf ++= valueToSqlString(args.next()) ++= parts.next()
      }
      Sql(buf.mkString)
    }
  }
}
