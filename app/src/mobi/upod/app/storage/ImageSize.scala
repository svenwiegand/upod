package mobi.upod.app.storage

import mobi.upod.android.view.DisplayMetrics
import mobi.upod.android.view.DisplayUnits._

import scala.annotation.tailrec

case class ImageSize(fileName: String, determineSize: DisplayMetrics => DensityIndependentPixels, smaller: Option[ImageSize]) {

  def size(implicit displayMetrics: DisplayMetrics): DensityIndependentPixels =
    determineSize(displayMetrics)
}

object ImageSize {
  val list = ImageSize("list", _ => 72.dp, None)
  val grid = ImageSize("grid", _ => 280.dp, Some(list))
  val smallestScreenDimension = ImageSize("smallestScreenDimension", _.smallestScreenDimension.dp, Some(grid))
  val largestScreenDimension = ImageSize("largestScreenDimension", _.largestScreenDimension.dp, smaller = Some(smallestScreenDimension))
  val full = ImageSize("full", _ => 0.dp, Some(largestScreenDimension))

  // special non cached sizes
  val largeList = ImageSize("largelist", _ => 80.dp, None)
  val hugeList = ImageSize("hugelist", _ => 96.dp, None)

  def closestFor(width: Int, height: Int)(implicit displayMetrics: DisplayMetrics): ImageSize = {

    @tailrec
    def findEqualOrLarger(requestedSize: Int, currentSize: ImageSize): ImageSize = {
      currentSize.smaller match {
        case Some(smallerSize) =>
          if (smallerSize.size.px < requestedSize) currentSize else findEqualOrLarger(requestedSize, smallerSize)
        case None =>
          currentSize
      }
    }

    findEqualOrLarger(math.max(width, height), full)
  }
}
