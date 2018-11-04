package mobi.upod.util

trait Duration[A] extends Any {
  def multiplyWithInt(value: A, factor: Int): A
  def toLong(value: A): Long

  def millis: A

  def ms = millis

  def seconds = multiplyWithInt(millis, Duration.MillisPerSecond)

  def second = seconds

  def minutes = multiplyWithInt(millis, Duration.MillisPerMinute)

  def minute = minutes

  def hours = multiplyWithInt(millis, Duration.MillisPerHour)

  def hour = hours

  def days = multiplyWithInt(millis, Duration.MillisPerDay)

  def day = days

  def fullHours: Int = (toLong(millis) / Duration.MillisPerHour).toInt

  def fullMinutesOfHour: Int =
    ((toLong(millis) % Duration.MillisPerHour) / Duration.MillisPerMinute).toInt

  def roundedMinutesOfHour: Int =
    (((toLong(millis) % Duration.MillisPerHour) + Duration.MillisPerMinute / 2) / Duration.MillisPerMinute).toInt

  def fullSecondsOfMinute: Int =
    ((toLong(millis) % Duration.MillisPerMinute) / Duration.MillisPerSecond).toInt

  def formatHoursAndMinutes: String =
    f"$fullHours%d:$roundedMinutesOfHour%02d"

  def formatHoursMinutesAndSeconds: String = fullHours match {
    case hours if hours > 0 => f"$hours%d:$fullMinutesOfHour%02d:$fullSecondsOfMinute%02d"
    case _ => f"$fullMinutesOfHour%d:$fullSecondsOfMinute%02d"
  }

  /** Formats this duration, so that the string is as wide as the formatted string of the specified `max` duration. */
  def formatFullAligned(max: Duration[A]): String = {
    (max.fullHours, max.fullMinutesOfHour) match {
      case (h, _) if h > 9 => f"$fullHours%02d:$fullMinutesOfHour%02d:$fullSecondsOfMinute%02d"
      case (h, _) if h > 0 => f"$fullHours%d:$fullMinutesOfHour%02d:$fullSecondsOfMinute%02d"
      case (_, m) if m > 9 => f"$fullMinutesOfHour%02d:$fullSecondsOfMinute%02d"
      case _ => f"$fullMinutesOfHour%d:$fullSecondsOfMinute%02d"
    }
  }
}

object Duration {
  val MillisPerSecond = 1000
  val MillisPerMinute = 60 * MillisPerSecond
  val MillisPerHour = 60 * MillisPerMinute
  val MillisPerDay = 24 * MillisPerHour

  implicit class IntDuration(val millis: Int) extends AnyVal with Duration[Int] with SimpleIntCalculator

  implicit class LongDuration(val millis: Long) extends AnyVal with Duration[Long] with SimpleLongCalculator

  implicit class DoubleDuration(val millis: Double) extends AnyVal with Duration[Double] with SimpleDoubleCalculator
}
