package mobi.upod.app.storage

private[storage] case class ColumnDefinition(name: Symbol, colType: String, defaultValue: Option[Any] = None)
