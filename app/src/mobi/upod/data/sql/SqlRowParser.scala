package mobi.upod.data.sql

import mobi.upod.android.database.Cursor
import mobi.upod.data.{DataElement, DataParser}

class SqlRowParser(tableName: Symbol, cursor: Cursor) extends DataParser {

  def hasNext: Boolean = !cursor.isLast && !cursor.isAfterLast

  def next(): DataElement = {
    if (!hasNext)
      throw new IllegalStateException("cursor already exceeded end")

    cursor.moveToNext()
    new SqlDataObject("", tableName, cursor)
  }
}
