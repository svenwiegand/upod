package mobi.upod.data

class DataTypeException(expected: Class[_], actual: DataElement)
  extends DataParseException(s"$actual is not of expected type $expected")
