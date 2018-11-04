package mobi.upod.app.gui.chapters

import java.io.{File, FileInputStream, IOException, RandomAccessFile}

import android.graphics.{Bitmap, BitmapFactory}
import android.util.Log
import mobi.upod.android.logging.Logging
import mobi.upod.android.os.AsyncTask
import mobi.upod.io._
import mobi.upod.media.ChapterImageReference

import scala.util.{Failure, Success, Try}

object ChapterImageLoader extends Logging {
  val DefaultDensity = 72

  def loadImage(file: File, imageReference: ChapterImageReference): Bitmap = {
    val bitmap = forCloseable(new RandomAccessFile(file, "r")) { f =>
      val stream = new FileInputStream(f.getFD)
      stream.skip(imageReference.offset)
      val options = new BitmapFactory.Options
      options.inDensity = DefaultDensity
      BitmapFactory.decodeStream(stream, null, options)
    }
    bitmap match {
      case null => throw new IOException(s"Failed to read image from file $file")
      case bmp => bmp
    }
  }

  def asyncLoadImage(file: File, imageReference: ChapterImageReference, onSuccess: Bitmap => Unit, onError: Throwable => Unit): Unit = {
    AsyncTask.execute(Try(loadImage(file, imageReference))) {
      case Success(bmp) =>
        Try(onSuccess(bmp))
      case Failure(ex) =>
        log.error(s"Failed to load chapter image from file '$file' with ref $imageReference")
        onError(ex)
    }
  }
}