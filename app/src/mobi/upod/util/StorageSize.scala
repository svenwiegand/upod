package mobi.upod.util

trait StorageSize[A] extends Any {
  def multiplyWithInt(value: A, factor: Int): A
  def divideByInt(value: A, divisor: Int): A

  def bytes: A

  def kb = multiplyWithInt(bytes, StorageSize.KiloByte)

  def mb = multiplyWithInt(bytes, StorageSize.MegaByte)

  def gb = multiplyWithInt(bytes, StorageSize.GigaByte)

  def inKb = divideByInt(bytes, StorageSize.KiloByte)

  def inMb = divideByInt(bytes, StorageSize.MegaByte)

  def inGb = divideByInt(bytes, StorageSize.GigaByte)
}

object StorageSize {
  val KiloByte = 1024
  val MegaByte = 1024 * KiloByte
  val GigaByte = 1024 * MegaByte

  implicit class IntStorageSize(val bytes: Int) extends AnyVal with StorageSize[Int] with SimpleIntCalculator

  implicit class LongStorageSize(val bytes: Long) extends AnyVal with StorageSize[Long] with SimpleLongCalculator

  implicit class DoubleStorageSize(val bytes: Double) extends AnyVal with StorageSize[Double] with SimpleDoubleCalculator
}
