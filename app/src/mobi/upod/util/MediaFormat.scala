package mobi.upod.util

import Duration._

object MediaPositionFormat extends Enumeration {
  type MediaPositionFormat = Value
  val CurrentAndDuration = Value("CurrentAndDuration")
  val RemainingAndDuration = Value("RemainingAndDuration")
  val CurrentAndRemaining = Value("CurrentAndRemaining")
}


object MediaFormat {
  import MediaPositionFormat._
  
  def format(value: Long, withSeconds: Boolean): String = {
    if (withSeconds)
      value.formatHoursMinutesAndSeconds
    else
      value.formatHoursAndMinutes    
  }
  
  def formatCurrentPosition(pos: MediaPosition, withSeconds: Boolean): String =
    format(pos.position, withSeconds)

  def formatRemaining(pos: MediaPosition, withSeconds: Boolean): String = {
    val remaining = Math.abs(pos.position - pos.duration)
    val number = format(remaining, withSeconds)
    if (pos.position < pos.duration) s"-$number" else number
  }
  
  def formatDuration(pos: MediaPosition, withSeconds: Boolean): String = 
    format(pos.duration, withSeconds)
  
  def formatPosition(pos: MediaPosition, format: MediaPositionFormat, withSeconds: Boolean): String = format match {
    case RemainingAndDuration => formatRemaining(pos, withSeconds)
    case _ => formatCurrentPosition(pos, withSeconds)
  }

  def formatDuration(pos: MediaPosition, format: MediaPositionFormat, withSeconds: Boolean): String = format match {
    case CurrentAndRemaining => formatRemaining(pos, withSeconds)
    case _ => formatDuration(pos, withSeconds)
  }

  def formatFullPosition(pos: MediaPosition, format: MediaPositionFormat, withSeconds: Boolean): String = {
    if (pos.position < 1000)
      formatDuration(pos, format, withSeconds)
    else
      formatPosition(pos, format, withSeconds) + " / " + formatDuration(pos, format, withSeconds)
  }
}
