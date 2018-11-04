package mobi.upod.util

import android.content.Context
import com.github.nscala_time.time.Imports._
import java.text.SimpleDateFormat
import mobi.upod.app.R

object DateTimeUtils {
  implicit class RichDateTime(val timestamp: DateTime) extends AnyVal {

    def formatRelativeDate(implicit context: Context): String = {
      val now = DateTime.now

      def isWithinDays(n: Int): Boolean = {
        val minDate = now.minusDays(n).toDateMidnight
        timestamp >= minDate
      }

      def format(resId: Int): String =
        new SimpleDateFormat(context.getString(resId)).format(timestamp.date)

      if (isWithinDays(-1))
        format(R.string.tomorrow)
      else if (isWithinDays(0))
        format(R.string.today)
      else if (isWithinDays(1))
        format(R.string.yesterday)
      else if (isWithinDays(6))
        format(R.string.date_week)
      else if (isWithinDays(180))
        format(R.string.date_year)
      else
        format(R.string.date_full)
    }
  }
}
