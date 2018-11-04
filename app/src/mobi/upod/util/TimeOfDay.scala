package mobi.upod.util

import com.github.nscala_time.time.Imports._
import org.joda.time.DateTime

case class TimeOfDay(hour: Int, minute: Int) {
  private lazy val str = f"$hour%02d:$minute%02d"

  require(hour >= 0 && hour < 24, s"$hour is no valid value for hour of day")
  require(minute >= 0 && minute < 60, s"$minute is no valid value for minute of hour")

  def next: DateTime = {
    val now = DateTime.now
    // We could use now.withHours in the next line, but this may crash on days where hour is forwarded from 2 to 3 due to DST
    // So we use now.minusHours which will handle this scenario for us correctly
    // (though best would be to use LocalDateTime instead)
    val nxt = now.minusHours(now.getHourOfDay - hour).withMinuteOfHour(minute)
    if (nxt > now) nxt else nxt + 1.day
  }

  override def toString: String = str
}

object TimeOfDay {
  def apply(str: String): TimeOfDay = str.split(':').toList match {
    case hour :: minute :: Nil => TimeOfDay(hour.toInt, minute.toInt)
    case _ => throw new IllegalArgumentException(s"$str is no valid time of day in the expected format 'HH:mm'")
  }
}