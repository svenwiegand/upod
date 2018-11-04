package mobi.upod.app.data

import mobi.upod.data.{Mapping, MappingProvider}
import org.joda.time.DateTime

case class Announcement(
  id: Long,
  modifiedDate: DateTime,
  startDate: Option[DateTime],
  endDate: Option[DateTime],
  freeUsersOnly: Boolean,
  primaryUrl: Option[String],
  secondyrUrl: Option[String],
  title: String,
  message: String,
  primaryButton: Option[String],
  secondaryButton: Option[String]
)

object Announcement extends MappingProvider[Announcement] {

  import Mapping._

  override val mapping: Mapping[Announcement] = map(
    "id" -> long,
    "modifiedDate" -> dateTime,
    "startDate" -> optional(dateTime),
    "endDate" -> optional(dateTime),
    "freeUsersOnly" -> boolean,
    "primaryUrl" -> optional(string),
    "secondaryUrl" -> optional(string),
    "title" -> string,
    "message" -> string,
    "primaryButton" -> optional(string),
    "secondaryButton" -> optional(string)
  )(apply)(unapply)
}