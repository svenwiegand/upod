package mobi.upod.android.os

import android.os.StatFs
import java.io.File
import mobi.upod.android.util.ApiLevel
import scala.util.{Success, Try}

trait FileSystem {

  def availableBytes: Long
}

object FileSystem {

  def apply(path: File): FileSystem = {
    if (ApiLevel >= ApiLevel.KitKat)
      new KitKatFileSystem(path)
    else
      new LegacyFileSystem(path)
  }

  private abstract class FileSystemBase(path: File) extends FileSystem {
    protected val statFs = Try(new StatFs(existingPath(path).getAbsolutePath))

    def existingPath(p: File): File = {
      if (p.exists)
        p
      else
        existingPath(p.getParentFile)
    }
  }

  private class LegacyFileSystem(path: File) extends FileSystemBase(path) {

    override def availableBytes = statFs match {
      case Success(fs) => fs.getAvailableBlocks.toLong * fs.getBlockSize
      case _ => 0l
    }
  }

  private class KitKatFileSystem(path: File) extends FileSystemBase(path) {

    override def availableBytes = statFs match {
      case Success(fs) => fs.getAvailableBytes
      case _ => 0l
    }
  }
}
