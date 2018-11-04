package mobi.upod.android.app

sealed abstract class ActivityState(val created: Boolean, val started: Boolean, val running: Boolean)

object ActivityState {
  object Launched extends ActivityState(false, false, false)
  object Created extends ActivityState(true, false, false)
  object Started extends ActivityState(true, true, false)
  object Running extends ActivityState(true, true, true)
  object Paused extends ActivityState(true, true, false)
  object Stopped extends ActivityState(true, false, false)
  object Destroyed extends ActivityState(false, false, false)
}
