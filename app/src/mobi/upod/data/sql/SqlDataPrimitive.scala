package mobi.upod.data.sql

import mobi.upod.android.database.Cursor
import mobi.upod.data.DataPrimitive
import mobi.upod.sql.Implicits._

private[sql] final class SqlDataPrimitive(name: String, cursor: Cursor, columnIndex: Int) extends DataPrimitive(name) {

  def asBoolean: Boolean = cursor.intAt(columnIndex) != 0

  def asDouble: Double = cursor.doubleAt(columnIndex)

  def asFloat: Float = cursor.floatAt(columnIndex)

  def asInt: Int = cursor.intAt(columnIndex)

  def asLong: Long = cursor.longAt(columnIndex)

  def asString: String = cursor.stringAt(columnIndex).unescape

  protected def debugInfo = Map("cursor" -> cursor)
}
