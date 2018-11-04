package mobi.upod

import java.io._

import mobi.upod.android.logging.Logger
import mobi.upod.util.StorageSize.IntStorageSize

package object io {

  val BufferSize: Int = 8.kb

  def closeWhenDone[A](closeable: Closeable*)(process: => A): A = try {
    process
  } finally {
    closeable.foreach(_.closeQuietly())
  }

  def forCloseable[A <: Closeable, B](closeable: A)(process: A => B): B = closeWhenDone(closeable) {
    process(closeable)
  }

  def forCloseable[A <: Closeable, B <: Closeable, C](closeableA: => A, closeableB: => B)(process: (A, B) => C): C = {
    forCloseable(closeableA) { a =>
      forCloseable(closeableB) { b =>
        process(a, b)
      }
    }
  }

  def copy(inputStream: => InputStream, outputStream: => OutputStream) {

    def copyBlock(in: InputStream, out: OutputStream, buffer: Array[Byte]): Unit = {
      val readBytes = in.read(buffer)
      if (readBytes > -1) {
        out.write(buffer, 0, readBytes)
        copyBlock(in, out, buffer)
      }
    }

    forCloseable(inputStream, outputStream) { (in, out) =>
      copyBlock(in, out, new Array[Byte](BufferSize))
    }
  }

  def copy(source: File, target: File): Unit =
    copy(new FileInputStream(source), new FileOutputStream(target))

  implicit class RichCloseable(val closeable: Closeable) extends AnyVal {

    def closeQuietly() {
      scala.util.control.Exception.ignoring(classOf[IOException]) {
        closeable.close()
      }
    }
  }

  implicit class Path(val path: String) extends AnyVal {
    import mobi.upod.util.Collections._

    def fileNameEncoded: String = FileNameEncoder.encode(path)

    def fileExtension: Option[String] = {
      val separatorIndex = path.lastIndexOf('.').validIndex
      separatorIndex.map(index => path.substring(index + 1))
    }
  }

  implicit class RichFile(val file: File) extends AnyVal {

    def fileExtension: Option[String] = file.getName.fileExtension

    def deleteRecursive() {
      if (file.isDirectory) {
        file.listFiles.foreach(_.deleteRecursive())
      }
      file.delete()
    }

    def deleteParentIfEmpty(): Boolean = {
      // normally file.getParentFile.delete() should be enough as it would only delete when directory is empty,
      // but I've the feeling, that this isn't the case on all platforms => so I am doing the check on my own here
      Option(file.getParentFile) match {
        case Some(parent) => Option(parent.list) match {
          case Some(children) if children.isEmpty => parent.delete()
          case _ => false
        }
        case _ =>
          false
      }
    }

    def copyTo(target: File): Unit =
      copy(file, target)

    def copyToDir(targetDir: File): File = {
      val targetFile = new File(targetDir, file.getName)
      copyTo(targetFile)
      targetFile
    }

    def copyRecursive(target: File): Unit = {
      val log = new Logger(classOf[RichFile])

      def copyRecursive(source: File, target: File): Unit = {
        if (source.isDirectory) {
          log.debug(s"recursive copy: creating directory $target")
          target.mkdirs()
          source.listFiles().toList.foreach(f => copyRecursive(f, new File(target, f.getName)))
        } else {
          log.debug(s"recursive copy: copying $source to $target")
          copy(source, target)
        }
      }

      copyRecursive(file, target)
    }

    def copyRecursiveToDir(targetDir: File): File = {
      val target = new File(targetDir, file.getName)
      copyRecursive(target)
      target
    }

    def listFilesRecursively: Seq[File] = {

      def listFilesRecursively(file: File, files: List[File]): List[File] = file match {
        case null =>
          Nil
        case f: File =>
          val childFiles = f.listFiles
          if (f.isDirectory && childFiles != null)
            childFiles.toList.flatMap(listFilesRecursively(_, files))
          else
            f :: files
      }

      listFilesRecursively(file, Nil)
    }
  }

  implicit class RichOutputStream(val stream: OutputStream) extends AnyVal {

    def buffered: BufferedOutputStream = stream match {
      case s: BufferedOutputStream => s
      case _ => new BufferedOutputStream(stream)
    }
  }
}
