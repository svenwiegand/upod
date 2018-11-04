package mobi.upod.data.sql

import com.google.gson.{JsonArray, JsonPrimitive, JsonElement, JsonObject}
import mobi.upod.data.json.JsonWriter
import mobi.upod.data.{MappingProvider, Mapping}
import mobi.upod.sql.Implicits._
import scala.collection.JavaConverters._
import mobi.upod.sql.Sql

class SqlGenerator[A] private (mapping: Mapping[A]) {
  private lazy val jsonWriter = JsonWriter(mapping)

  private def expand(name: String, element: JsonElement): Seq[(String, String)] = element match {
    case json: JsonObject =>
      expand(json, name + '_')
    case json: JsonArray =>
      Seq(name -> s"'${JsonWriter.gson.toJson(json).escapeJsonArray}'")
    case json: JsonPrimitive if json.isBoolean =>
      val value = if (json.getAsBoolean) "1" else "0"
      Seq(name -> value)
    case json: JsonPrimitive if json.isNumber =>
      Seq(name -> json.getAsNumber.toString)
    case json: JsonPrimitive if json.isString =>
      Seq(name -> s"'${json.getAsString.escape}'")
    case _ =>
      Seq(name -> "NULL")
  }

  private def expand(element: JsonObject, baseName: String = ""): Seq[(String, String)] = {
    element.entrySet.asScala.toSeq.flatMap(entry => expand(baseName + entry.getKey, entry.getValue))
  }

  private def expand(data: A): Seq[(String, String)] = {
    jsonWriter.writeJson(data) match {
      case json: JsonObject => expand(json)
      case _ => throw new IllegalArgumentException("mapping must specify an object")
    }
  }

  def generateInsertValues(data: A, additionalValues: A => Seq[(String, String)]): Sql = {
    val namedValues = expand(data).toMap ++ additionalValues(data)
    val names = namedValues.map(_._1).mkString(", ")
    val values = namedValues.map(_._2).mkString(", ")
    Sql(s"($names) VALUES ($values)")
  }

  def generateInsertValues(data: Iterable[A], additionalValues: A => Seq[(String, String)]): Sql = {
    require(!data.isEmpty)
    val first = generateInsertValues(data.head, additionalValues)
    if (data.tail.isEmpty)
      first
    else
      Sql(first.sql + data.tail.map(d => (expand(d).toMap ++ additionalValues(d)).map(_._2).mkString("(", ", ", ")")).mkString(", ", ", ", ""))
  }

  def generateUpdateValues(
    data: A,
    additionalValues: A => Seq[(String, String)] = _ => Seq(),
    columnFilter: String => Boolean = _ => true): Sql = {
    val expanded = expand(data).toMap ++ additionalValues(data)
    val filtered = expanded.filter(t => columnFilter(t._1))
    Sql(filtered.map(pair => s"${pair._1}=${pair._2}").mkString(", "))
  }
}

object SqlGenerator {

  def apply[A](mapping: Mapping[A]) = new SqlGenerator[A](mapping)

  def apply[A](mappingProvider: MappingProvider[A]) = new SqlGenerator[A](mappingProvider.mapping)
}
