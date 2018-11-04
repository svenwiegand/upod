package mobi.upod.app.services.playback

case class VideoSize(width: Int, height: Int) {

  lazy val aspectRatio: Float =
    width.toFloat / height

  def forBoundingBox(box: VideoSize): VideoSize = {

    def scaleToMaxHeight =
      VideoSize(aspectRatio * box.height, box.height)

    def scaleToMaxWidth =
      VideoSize(box.width, box.width / aspectRatio)

    if (box.aspectRatio < aspectRatio)
      scaleToMaxWidth
    else
      scaleToMaxHeight
  }
}

object VideoSize {

  def apply(width: Float, height: Float): VideoSize =
    VideoSize(width.toInt, height.toInt)
}
