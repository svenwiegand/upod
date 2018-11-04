package mobi.upod.data.sql

import mobi.upod.android.database.{Cursor => DbCursor}
import mobi.upod.data.Mapping
import mobi.upod.util.Cursor

class SqlReader[A] private (tableName: Symbol, mapping: Mapping[A], cursor: DbCursor) extends Cursor[A] {
  private val parser = new SqlRowParser(tableName, cursor)

  def hasNext: Boolean = parser.hasNext

  def next(): A = mapping.read(parser.next())

  def close() {
    cursor.close()
  }
}

object SqlReader {

  def apply[A](tableName: Symbol, mapping: Mapping[A], cursor: DbCursor): SqlReader[A] =
    new SqlReader(tableName, mapping, cursor)
}
