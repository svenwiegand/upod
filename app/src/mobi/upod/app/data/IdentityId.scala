package mobi.upod.app.data

import mobi.upod.data.{Mapping, MappingProvider}

case class IdentityId(providerId: String, userId: String)

object IdentityId extends MappingProvider[IdentityId] {
  import Mapping._

  val mapping = map(
    "providerId" -> string,
    "userId" -> string
  )(apply)(unapply)
}