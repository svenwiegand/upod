package mobi.upod.app.services.playback

sealed trait SleepTimerMode

object SleepTimerMode {

  object Off extends SleepTimerMode
  object Chapter extends SleepTimerMode
  object Episode extends SleepTimerMode

  final case class Timer(millis: Long, startTime: Long = System.currentTimeMillis()) extends SleepTimerMode {
    val offTime = startTime + millis

    def remaining: Long = offTime - System.currentTimeMillis()

    def isFinished: Boolean = remaining <= 0
  }
}