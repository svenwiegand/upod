package mobi.upod.android.database

import android.database.{Cursor => DbCursor}
import mobi.upod.android.logging.Logging

import scala.collection.mutable

class Cursor private (c: DbCursor, id: Long) {

  def typeAt(n: Int) = c.getType(n)

  def isNullAt(n: Int) = c.isNull(n)

  def blobAt(n: Int) = c.getBlob(n)

  def doubleAt(n: Int) = c.getDouble(n)

  def floatAt(n: Int) = c.getFloat(n)

  def intAt(n: Int) = c.getInt(n)

  def longAt(n: Int) = c.getLong(n)

  def shortAt(n: Int) = c.getShort(n)

  def stringAt(n: Int) = c.getString(n)

  def columnCount = c.getColumnCount

  def columnIndex(columnName: String): Option[Int] = {
    val index = c.getColumnIndex(columnName)
    if (index == -1) None else Some(index)
  }

  def columnIndexOrThrow(columnName: String): Int = c.getColumnIndexOrThrow(columnName)

  def columnName(columnIndex: Int): String = c.getColumnName(columnIndex)

  def columnNames = c.getColumnNames

  def count = c.getCount

  def size = c.getCount

  def position = c.getPosition

  def isFirst = c.isFirst

  def isLast = c.isLast

  def isBeforeFirst = c.isBeforeFirst

  def isAfterLast = c.isAfterLast

  def move(offset: Int) = c.move(offset)

  def moveToFirst() = c.moveToFirst()

  def moveToPrevious() = c.moveToPrevious()

  def moveToNext() = c.moveToNext()

  def moveToLast() = c.moveToLast()

  def moveToPosition(position: Int) = c.moveToPosition(position)

  def close() = {
    c.close()
    Cursor.onClose(id)
  }
}

object Cursor extends Logging {
  private val MaxSize = 10
  private var _nextId: Long = 0
  private var _openCursors = mutable.LinkedHashMap[Long, String]()

  def apply(c: DbCursor): Cursor = {

    def withLogging() = this.synchronized {
      _nextId += 1
      if (_openCursors.size < MaxSize) {
        _openCursors += _nextId -> Thread.currentThread.getStackTrace.drop(4).map(_.toString).mkString("\n")
      }
      log.debug(s"created cursor #${_nextId}")
      new Cursor(c, _nextId)
    }

    if (log.isDebugEnabled) withLogging() else new Cursor(c, 0)
  }

  private def onClose(id: Long): Unit = {

    def withLogging() = this.synchronized {
      _openCursors -= id
      if (_openCursors.nonEmpty) {
        log.debug(s"oldest $MaxSize database cursors left open: [${_openCursors.keys.mkString(", ")}]")
      }
    }

    log.debug(s"closing database cursor #$id")
    if (id != 0 && log.isDebugEnabled) {
      withLogging()
    }
  }

  def logOpenCursors(): Unit = if (log.isDebugEnabled && _openCursors.nonEmpty) {
    log.debug(s"oldest $MaxSize database cursors left open: [${_openCursors.keys.mkString(", ")}]")
    _openCursors.foreach(c => log.debug(s"unclosed database cursor #${c._1} created at\n${c._2}"))
  }
}