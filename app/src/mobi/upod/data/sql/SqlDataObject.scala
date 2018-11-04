package mobi.upod.data.sql

import mobi.upod.android.database.Cursor
import mobi.upod.data.{NoData, DataElement, DataObject}

private[sql] final class SqlDataObject(name: String, tableName: Symbol, cursor: Cursor, baseName: String = "")
  extends DataObject(name) {
  private val columnNames: IndexedSeq[String] = indexedColumnNames

  private def indexedColumnNames = {
    val tablePrefix = tableName.name + '.'
    IndexedSeq(cursor.columnNames: _*) map { name =>
      if (name.startsWith(tablePrefix))
        name.substring(tablePrefix.length)
      else
        name
    }
  }

  def apply(name: String): DataElement = {
    val fullName = baseName + name
    columnNames.indexOf(fullName) match {
      case index if index >= 0 => getConcreteData(name, index)
      case _ => getObjectOrNoData(fullName)
    }
  }

  private def getConcreteData(fullName: String, index: Int): DataElement = {
    if (cursor.isNullAt(index))
      NoData(fullName)
    else {
      SqlDataSequence(fullName, cursor.stringAt(index)) match {
        case Some(sequence) => sequence
        case None => new SqlDataPrimitive(fullName, cursor, index)
      }
    }
  }

  private def getObjectOrNoData(fullName: String): DataElement = {
    val subName = fullName + '_'
    if (columnNames.exists(_.startsWith(subName)))
      new SqlDataObject(fullName, tableName, cursor, subName)
    else
      NoData(fullName)
  }

  protected def debugInfo = Map("cursor" -> cursor)
}
