package mobi.upod.util

trait SimpleIntCalculator extends Any {

  def zero = 0

  def multiplyWithInt(value: Int, factor: Int) = value * factor

  def multiply(value: Int, factor: Int) = value * factor

  def divideByInt(value: Int, divisor: Int) = value / divisor

  def divide(value: Int, divisor: Int) = value / divisor

  def toLong(value: Int) = value.toLong
}

trait SimpleLongCalculator extends Any {

  def zero = 0l

  def multiplyWithInt(value: Long, factor: Int) = value * factor

  def multiply(value: Long, factor: Long) = value * factor

  def divideByInt(value: Long, divisor: Int) = value / divisor

  def divide(value: Long, divisor: Long) = value / divisor

  def toLong(value: Long) = value
}

trait SimpleDoubleCalculator extends Any {

  def zero = 0.0

  def multiplyWithInt(value: Double, factor: Int) = value * factor

  def multiply(value: Double, factor: Double) = value * factor

  def divideByInt(value: Double, divisor: Int) = value / divisor

  def divide(value: Double, divisor: Double) = value / divisor

  def toLong(value: Double) = value.toLong
}

