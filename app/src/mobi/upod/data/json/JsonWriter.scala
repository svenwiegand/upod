package mobi.upod.data.json

import com.google.gson.{JsonElement, Gson}
import mobi.upod.data.{MappingProvider, Mapping}

class JsonWriter[A] private (mapping: Mapping[A]) extends ElementToJsonConverter {
  import JsonWriter._

  def writeJson(data: A): JsonElement =
    convert(mapping.write(JsonElementFactory, "", data))

  def writeJson(data: A, writer: Appendable): Unit =
    gson.toJson(writeJson(data), writer)

  def writeJson(data: Iterator[A], writer: Appendable): Unit = {
    writer.append("[\n  ")
    if (data.hasNext)
      writeJson(data.next(), writer)

    data foreach { item =>
      writer.append(",\n  ")
      writeJson(item, writer)
    }
    writer.append("\n]")
  }

  def writeJson(data: Iterable[A], writer: Appendable): Unit =
    writeJson(data.iterator, writer)

  def writeString(data: A): String =
    gson.toJson(writeJson(data))
}

object JsonWriter {
  val gson = new Gson()

  def apply[A](mapping: Mapping[A]) = new JsonWriter[A](mapping)

  def apply[A](mappingProvider: MappingProvider[A]) = new JsonWriter[A](mappingProvider.mapping)
}
