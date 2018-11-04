import java.io._
import scala.sys.process._

object ImageConverter {
  val rootDir = new File("images/export")
  var targetResourceDir = new File("app/res")

  object DensityFactor {
    val mdpi = 1.0
    val hdpi = 1.5
    val xhdpi = 2
    val xxhdpi = 3
  }

  implicit class DensityAwareSize(val mdpi: Int) {

    private def scale(factor: Double): Int = (factor * mdpi).toInt

    def hdpi = scale(DensityFactor.hdpi)

    def xhdpi = scale(DensityFactor.xhdpi)

    def xxhdpi = scale(DensityFactor.xxhdpi)
  }

  implicit class RichFile(val file: File) {

    def absolutePath = file.getAbsolutePath

    def name = file.getName

    def baseName = file.getName.lastIndexOf('.') match {
      case dotIndex if dotIndex > 0 => file.getName.substring(0, dotIndex)
      case _ => file.getName
    }

    def extension: String = file.getName.lastIndexOf('.') match {
      case dotIndex if dotIndex > 0 => file.getName.substring(dotIndex)
      case _ => ""
    }

    def tempFile: File = File.createTempFile(name, extension)
  }

  private def changed(src: File, dest: File): Boolean =
    !dest.exists || dest.lastModified() < src.lastModified()

  private class ExecutionFailedException(val command: String, val exitCode: Int)
    extends Exception("Command '" + command + "' failed with exit code " + exitCode)

  private def executeShellCmd(command: String) {
    val exitCode = command.!
    if (exitCode != 0)
      throw new ExecutionFailedException(command, exitCode)
  }

  private def executeShellCmd(command: List[String]) {
    val fullCmd = "cmd" :: "/c" :: command
    val exitCode = fullCmd.run().exitValue()
    if (exitCode != 0)
      throw new ExecutionFailedException(command.mkString(" "), exitCode)
  }

  private def convert(src: File, dest: File, args: String*) {
    executeShellCmd("convert.exe" :: src.absolutePath :: args.toList ::: dest.getAbsolutePath :: Nil)
  }

  private def scaleImage(src: File, dest: File, targetHeight: Int) {
    convert(src, dest,
            "-define", "png:color-type=6",
            "-resize", s"x$targetHeight")
  }

  private def overlayImage(background: File, overlay: File, dest: File) {
    executeShellCmd(s"""composite.exe "$overlay" "$background" -gravity center "$dest" """)
  }

  private def convertGrayImage(src: File, dest: File, targetHeight: Int, grayValue: Int, opacity: Double) {
    val brightness = 100 * grayValue / 0xff
    convert(src, dest,
            "-define", "png:color-type=6",
            "-resize", s"x$targetHeight",
            "-brightness-contrast", brightness.toString,
            "-channel", "Alpha", "-evaluate", "Multiply", opacity.toString)
  }

  class Image(name: String, height: DensityAwareSize)(convert: (File, File, Int, String) => Unit) {
    private val typeDir = new File(rootDir, name)

    def process() {
      typeDir.listFiles filter { file => file.isFile && !file.name.endsWith("dpi.png")} foreach { processImage }
    }

    private def processImage(img: File) {
      println(s"processing ${img.getName}")

      convertImage(img, "hdpi", height.hdpi)
      convertImage(img, "xhdpi", height.xhdpi)
      convertImage(img, "xxhdpi", height.xxhdpi)
    }

    private def convertImage(src: File, destDpi: String, height: Int) {
      print(s"   ${destDpi.padTo("xxhdpi".length, ' ')} ... ")
      val dest = new File(new File(targetResourceDir, s"drawable-$destDpi"), src.name)
      if (changed(src, dest)) {
        convert(src, dest, height, destDpi)
        println(s"created")
      } else {
        println("up-to-date")
      }
    }
  }

  object GrayImage {
    def apply(name: String, height: DensityAwareSize) = new Image(name, height)({
      (src, dest, height, densityName) =>
        convertGrayImage(src, new File(dest.getParentFile, s"${src.baseName}_light${src.extension}"), height, 0x73, 1)
        convertGrayImage(src, dest, height, 0xff, 1)
    })
  }

  object Image {
    def apply(name: String, height: DensityAwareSize) = new Image(name, height)({
      (src, dest, height, densityName) =>
        scaleImage(src, dest, height)
    })
  }

  object NinePatchImage {
    def getNinePatch(src: File, densityName: String) =
      new File(src.getParentFile, s"${src.baseName}.$densityName.png")

    def apply(name: String, height: DensityAwareSize, ninePatch: (File, String) => File) = new Image(name, height)({
      (src, dest, height, densityName) =>
        val scaledImage = src.tempFile
        scaleImage(src, scaledImage, height)
        overlayImage(ninePatch(src, densityName), scaledImage, dest)
        scaledImage.delete()
    })

    def apply(name: String, height: DensityAwareSize): Image = apply(name, height, { (src: File, densityName: String) =>
      new File(src.getParentFile, s"${src.baseName}.$densityName.png")
    })
  }

  object SelectorImage {

    def apply(name: String, height: DensityAwareSize) = NinePatchImage(name, height, { (src, densityName) =>
      val baseName = src.baseName.replace("_default", "").replace("_disabled", "").replace("_focused", "").replace("_pressed", "")
      new File(src.getParentFile, s"$baseName.$densityName.png")
    })
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  def processImages() {
    Image("logo", 48).process()
    Image("navigation", 24).process()
    GrayImage("actions", 24).process()
    Image("prefs", 24).process()
    Image("actions_raw", 24).process()
    Image("notifications", 24).process()
    Image("item_indicators", 16).process()

    GrayImage("media_control", 48).process()
    Image("media_control_overlay", 48).process()

    Image("context_menu_button", 22).process()
    Image("drag_handle", 18).process()

    NinePatchImage("button", 32).process()
    NinePatchImage("counter", 32).process()
    NinePatchImage("podcast_grid_item_background", 24).process()
    NinePatchImage("podcast_image_background", 24).process()
    NinePatchImage("pane_shadow", 24).process()
  }
}

ImageConverter.processImages()

