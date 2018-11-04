package mobi.upod.app.storage

import android.database.sqlite.SQLiteDatabase

class Database private[storage](val db: SQLiteDatabase, helper: DatabaseHelper) {

  def newTransaction[B](block: => B): B = {
    db.beginTransaction()
    try {
      val result = block
      db.setTransactionSuccessful()
      result
    } finally {
      if (db.inTransaction()) {
        db.endTransaction()
      }
    }
  }

  def inTransaction[B](block: => B): B = {
    if (db.inTransaction())
      block
    else
      newTransaction(block)
  }

  def disableTriggers() = helper.disableTriggers()

  def enableTriggers() = helper.enableTriggers()

  def withoutTriggers[B](block: => B): B = {
    disableTriggers()
    val result = block
    enableTriggers()
    result
  }

  def newTransactionWithoutTriggers[B](block: => B): B =
    newTransaction(withoutTriggers(block))
}

object Database {
  implicit def databaseToSQLiteDatabase(database: Database): SQLiteDatabase = database.db
}
