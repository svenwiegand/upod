package mobi.upod.app.data

import mobi.upod.data.{Mapping, MappingProvider}

final case class Identity(id: IdentityId, email: Option[String], lastLogin: Long)

object Identity extends MappingProvider[Identity] {
  import Mapping._

  val mapping = map(
    "id" -> IdentityId.mapping,
    "email" -> optional(string),
    "lastLogin" -> long
  )(apply)(unapply)
}
