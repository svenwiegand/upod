package mobi.upod.data

abstract class DataSequence(name: String) extends DataElement(name) with Iterable[DataElement]
