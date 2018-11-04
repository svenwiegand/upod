package mobi.upod.util

trait Permille[A] extends Any {

  def zero: A

  def multiplyWithInt(value: A, factor: Int): A

  def multiply(value: A, factor: A): A

  def divideByInt(value: A, divisor: Int): A

  def divide(value: A, divisor: A): A

  def value: A

  def permille(max: A): A = if (max == 0) zero else divide(multiplyWithInt(value, Permille.PermilleMax), max)

  def fromPermille(max: A): A = divideByInt(multiply(max, value), Permille.PermilleMax)
}

object Permille extends {
  val PermilleMax = 1000

  implicit class IntPermille(val value: Int) extends AnyVal with Permille[Int] with SimpleIntCalculator

  implicit class LongPermille(val value: Long) extends AnyVal with Permille[Long] with SimpleLongCalculator

  implicit class DoublePermille(val value: Double) extends AnyVal with Permille[Double] with SimpleDoubleCalculator
}
