package mobi.upod.sql

import mobi.upod.sql.Implicits.{SqlStringInterpolator, SqlValueString}

trait SqlApp {

  implicit def stringToSqlValueString(str: String) = new SqlValueString(str)

  implicit def sqlStringInterpolator(str: StringContext) = new SqlStringInterpolator(str)
}
