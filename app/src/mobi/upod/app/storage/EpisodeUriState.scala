package mobi.upod.app.storage

/** Identifies whether we are already using the current episode URI mechanism (taken from episode GUID if available)
  * or the old one (app version < 4.4.0)
  */
object EpisodeUriState extends Enumeration {
  type EpisodeUriState = Value
  type Type = EpisodeUriState

  /** At least on this device the episode URIs need to be updated. */
  val LocalUriUpdateRequired = Value

  /** The URIs are up-to-date */
  val UpToDate = Value
}