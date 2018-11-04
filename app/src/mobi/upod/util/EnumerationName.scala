package mobi.upod.util


trait EnumerationName extends Enumeration {

  implicit def valueToValueName(value: Value) = EnumerationName.ValueName(value)
}

object EnumerationName {

  implicit class ValueName[A <: Enumeration#Value](val value: A) extends AnyVal {
    def name = value.toString
  }
}
