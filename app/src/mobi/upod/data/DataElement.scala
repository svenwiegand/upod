package mobi.upod.data

import scala.reflect.ClassTag

abstract class DataElement(val name: String) {
  private def as[A](implicit classTag: ClassTag[A]): A = this match {
    case target: A => target
    case other => throw new DataTypeException(classTag.runtimeClass, other)
  }

  def asDataPrimitive = as[DataPrimitive]

  def asDataObject = as[DataObject]

  def asDataSequence = as[DataSequence]

  protected def debugInfo: Map[String, Any]

  private def debugInfoString = debugInfo.map(entry => s"{'${entry._1}' -> '${entry._2}'}").mkString(";")

  override def toString = s"${getClass.getSimpleName}($name)[$debugInfoString]"
}
