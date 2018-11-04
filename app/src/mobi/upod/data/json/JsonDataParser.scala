package mobi.upod.data.json

import com.google.gson.{JsonElement, JsonStreamParser}
import mobi.upod.data.{DataElement, DataParser}

class JsonDataParser(json: Iterator[JsonElement]) extends DataParser with JsonToElementConverter {

  def hasNext: Boolean = json.hasNext

  def next(): DataElement = convert(json.next())
}
