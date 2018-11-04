package mobi.upod.app.storage

import android.database.sqlite.SQLiteStatement
import com.escalatesoft.subcut.inject.{Injectable, BindingModule}
import mobi.upod.android.database.{Cursor => DbCursor}
import mobi.upod.android.logging.Logging
import mobi.upod.data.Mapping
import mobi.upod.data.sql.{SqlReader, SqlGenerator}
import mobi.upod.sql.{Sql, SqlApp}
import mobi.upod.util.Cursor

abstract class Dao[A](
    val table: Symbol,
    dbHelper: DatabaseHelper,
    entityMapping: Mapping[A])(
    implicit val bindingModule: BindingModule)
  extends Injectable
  with SqlApp
  with Logging {

  val tableName = table.name

  protected val PRIMARY_KEY = "INTEGER PRIMARY KEY AUTOINCREMENT"
  protected val PRIMARY_KEY_TEXT = "TEXT PRIMARY KEY"
  protected val PRIMARY_KEY_INT = "INTEGER PRIMARY KEY"
  protected val INTEGER = "INTEGER"
  protected val REAL = "REAL"
  protected val TEXT = "TEXT"
  protected val UNIQUE_TEXT = "TEXT UNIQUE"
  protected val emptySelectionArgs = Array.empty[String]

  protected val OrReplace = sql"OR REPLACE"
  protected val OrIgnore = sql"OR IGNORE"
  protected val OrFail = sql"OR FAIL"

  implicit val standardMapping = entityMapping
  protected val sqlGenerator = SqlGenerator(entityMapping)
  protected implicit lazy val db = dbHelper.writable

  protected def columns: Map[Symbol, String]

  protected def tableConstraints: Seq[Sql] = Seq()

  protected def indices: Seq[Index] = Seq()

  protected val triggers: Seq[Trigger] = Seq()

  protected[storage] def upgrade(database: Database, oldVersion: Int, newVersion: Int) {
    // do nothing by default
  }

  protected[storage] def create(database: Database) {
    drop(database)
    createTable(database)
    createIndices(database)
    createTriggers(database)
  }

  protected[storage] def recreate(database: Database) {
    drop(database)
    create(database)
  }

  protected def createTable(database: Database) {
    val columnDefinition = columns.map(entry => entry._1.name + " " + entry._2).mkString(",\n")
    val tableConstraintsString = if (tableConstraints.isEmpty)"" else tableConstraints.mkString(", \n", ",\n", "")
    val createStatement = s"CREATE TABLE $tableName ($columnDefinition$tableConstraintsString)"
    logSql("create", createStatement)
    database.execSQL(createStatement)
  }

  protected[storage] def drop(database: Database) {
    dropTriggers(database)
    dropIndices(database)
    database.execSQL(sql"DROP TABLE IF EXISTS $table")
  }

  private[storage] def deleteAll() {
    db.delete(tableName, null, null)
  }

  protected def verifyInTransaction(database: Database): Unit = if (!database.db.inTransaction()) {
    throw new IllegalStateException("Update must be executed in a transaction")
  }

  protected def verifyInTransaction(): Unit =
    verifyInTransaction(db)

  private def execStatement[T](database: Database, statement: Sql, exec: SQLiteStatement => T): T = {
    logSql("execSql", statement)
    verifyInTransaction(database)
    val stmt = database.compileStatement(statement)
    exec(stmt)
  }

  def execSql(database: Database, statement: Sql): Unit =
    execStatement(database, statement, _.execute())

  def execSql(statement: Sql): Unit =
    execSql(db, statement)

  def execInsert(database: Database, statement: Sql): Option[Long] = execStatement(database, statement, { stmt =>
    stmt.executeInsert() match {
      case id if id >= 0 => Some(id)
      case id => None
    }
  })

  def execInsert(statement: Sql): Option[Long] =
    execInsert(db, statement)

  def execUpdateOrDelete(database: Database, statement: Sql): Int =
    execStatement(database, statement, _.executeUpdateDelete())

  def execUpdateOrDelete(statement: Sql): Int =
    execUpdateOrDelete(db, statement)

  def execUpdate(statement: Sql): Int =
    execUpdateOrDelete(statement)

  def execDelete(statement: Sql): Int =
    execUpdateOrDelete(statement)

  private def rawQuery(query: Sql): DbCursor = {
    logSql("query", query)
    DbCursor(db.rawQuery(query, emptySelectionArgs))
  }

  //
  // inserts and updates
  //

  protected def generatedColumns(entity: A): Seq[(String, String)] =
    Seq()

  protected def insert[T](onConflict: Sql, entity: T, generator: SqlGenerator[T], generateColumns: T => Seq[(String, String)]): Option[Long] = {
    val valueDefs = generator.generateInsertValues(entity, generateColumns)
    execInsert(sql"INSERT $onConflict INTO $table $valueDefs")
  }

  private def insert(onConflict: Sql, entity: A): Option[Long] =
    insert(onConflict, entity, sqlGenerator, generatedColumns)

  def save(entity: A): Option[Long] =
    insert(OrReplace, entity)

  def save(entities: Iterable[A]): Unit =
    entities.foreach(save)

  def insertOrIgnore(entity: A): Option[Long] =
    insert(OrIgnore, entity)

  def insertOrFail(entity: A): Option[Long] =
    insert(OrFail, entity)

  def insertOrReplace(entity: A): Option[Long] =
    save(entity)

  protected def deleteWhere(where: Sql) {
    execSql(sql"DELETE FROM $table WHERE $where")
  }

  //
  // query stuff
  //

  protected def findOne[B](query: Sql, mapping: Mapping[B] = standardMapping): Option[B] = {
    withReader(rawQuery(query), mapping) { reader =>
      if (reader.hasNext)
        Some(reader.next())
      else
        None
    }
  }

  protected def findMultiple[B](query: Sql, mapping: Mapping[B] = standardMapping): Cursor[B] =
    SqlReader(table, mapping, rawQuery(query))

  //
  // upgrade stuff
  //

  protected def schemaUpgrade(database: Database)(block: => Unit) {
    dropTriggers(database)
    dropIndices(database)
    block
    createIndices(database)
    createTriggers(database)
  }

  protected def addColumns(database: Database, columns: ColumnDefinition*) {
    columns.foreach { column =>
      execSql(database, sql"ALTER TABLE $table ADD COLUMN ${column.name} ${column.colType}")
      column.defaultValue match {
        case Some(value) => execSql(database, sql"UPDATE $table SET ${column.name}=$value")
        case None =>
      }
    }
  }

  protected def recreateWithContent(database: Database): Unit = {
    val temp = Sql("temp_" + table.name)

    def renameToTemp(): Unit =
      database.execSQL(sql"ALTER TABLE $table RENAME TO $temp")

    def copyContent(): Unit = {
      val columnNames = Sql(columns.keys.map(_.name).mkString(", "))
      val statement = sql"INSERT INTO $table ($columnNames) SELECT $columnNames FROM $temp"
      database.execSQL(statement)
    }

    def dropTemp(): Unit =
      database.execSQL(sql"DROP TABLE $temp")

    renameToTemp()
    createTable(database)
    copyContent()
    dropTemp()
  }


  //
  // index stuff
  //

  protected def createIndices(database: Database) {
    indices.foreach(createIndex(database, _))
  }

  protected def dropIndices(database: Database) {
    indices.foreach(index => dropIndex(database, index.name.name))
  }

  protected def recreateIndices(database: Database) {
    dropIndices(database)
    createIndices(database)
  }

  protected def createIndex(database: Database, index: Index) {
    execSql(database, index.sql)
  }

  protected def dropIndex(database: Database, name: String) {
    val sql = Sql(s"DROP INDEX IF EXISTS $name")
    execSql(database, sql)
  }

  //
  // triggers
  //

  protected def createTriggers(database: Database) {
    triggers.foreach(trigger => execSql(database, trigger.createSql))
  }

  protected def dropTriggers(database: Database) {
    triggers.foreach(trigger => execSql(database, trigger.dropSql))
  }

  protected def recreateTriggers(database: Database): Unit = {
    dropTriggers(database)
    createTriggers(database)
  }

  def enableTriggers() {
    createTriggers(db)
  }

  def disableTriggers() {
    dropTriggers(db)
  }

  //
  // helpers
  //

  private def withCursor[B](cursor: DbCursor)(block: DbCursor => B): B = {
    try {
      block(cursor)
    } finally {
      cursor.close()
    }
  }

  private def withReader[B, C](cursor: DbCursor, mapping: Mapping[B] = standardMapping)(block: SqlReader[B] => C): C = withCursor(cursor) { cursor =>
    block(SqlReader(table, mapping, cursor))
  }

  protected def logSql(prefix: String, statement: => String): Unit =
    log.debug(s"$prefix: ${statement.take(1000)}")

  //
  // SQL functions
  //
  protected val NOW = sql"(strftime('%s','now') * 1000)"
}

object Dao {

  implicit def daoToDatabase(dao: Dao[_]): Database = dao.db
}