package mobi.upod.app.services.net

object ConnectionState extends Enumeration {
  type ConnectionState = Value
  val Unconnected = Value("Unconnected")
  val Metered = Value("Metered")
  val Full = Value("Full")
}
