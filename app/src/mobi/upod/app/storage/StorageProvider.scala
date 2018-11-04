package mobi.upod.app.storage

import android.content.Context
import android.content.pm.PackageManager
import android.os.Environment
import java.io.File
import android.support.v4.content.ContextCompat
import mobi.upod
import mobi.upod.android.logging.Logging
import mobi.upod.android.os.FileSystem
import mobi.upod.android.util.ApiLevel
import mobi.upod.app.R
import mobi.upod.util.EnumerationName
import scala.io.Source

sealed trait StorageProvider extends Logging {

  import StorageProvider._

  val id: StorageProvider.StorageProviderType
  protected val context: Context
  lazy val storageName = id match {
    case Internal => context.getString(R.string.storage_internal)
    case External => context.getString(R.string.storage_external)
    case ExternalSecondary => context.getString(R.string.storage_external_secondary)
  }

  protected def getDirectory(dirType: String): File

  def coverartDirectory: File =
    getDirectory(Coverart)

  def podcastDirectory: File =
    getDirectory(Podcasts)

  def state: StorageState.StorageState

  def hasStorageAccessPermission: Boolean

  final def readable: Boolean =
    state != StorageState.NotAvailable && hasStorageAccessPermission

  def writable: Boolean =
    state == StorageState.Writable && hasStorageAccessPermission

  private lazy val fileSystem = FileSystem(podcastDirectory)

  def availableBytes: Long =
    fileSystem.availableBytes

  def whenReadable[A](operation: StorageProvider => A): Option[A] =
    if (readable) Some(operation(this)) else None

  def whenWritable[A](operation: StorageProvider => A): Option[A] =
    if (writable) Some(operation(this)) else None
}

private[storage] class InternalStorageProvider(protected val context: Context) extends StorageProvider {

  val id = StorageProvider.Internal

  protected def getDirectory(dirType: String) = new File(context.getFilesDir, dirType)

  override def state = StorageState.Writable

  override def hasStorageAccessPermission: Boolean = true
}

private[storage] trait ExternalStorageProviderBase extends StorageProvider {

  protected def storageState: String

  override def state = storageState match {
    case Environment.MEDIA_MOUNTED => StorageState.Writable
    case Environment.MEDIA_MOUNTED_READ_ONLY => StorageState.Readable
    case _ => StorageState.NotAvailable
  }

  override def hasStorageAccessPermission: Boolean =
    StorageProvider.hasExternalStoragePermissions(context)
}

private[storage] class ExternalStorageProvider(protected val context: Context) extends ExternalStorageProviderBase {

  val id = StorageProvider.External

  protected def getDirectory(dirType: String) = context.getExternalFilesDir(dirType)

  override protected def storageState = Environment.getExternalStorageState
}

private[storage] class SecondaryExternalStorageProvider(protected val context: Context) extends ExternalStorageProviderBase {

  val id = StorageProvider.ExternalSecondary

  override protected def getDirectory(dirType: String) = Option(context.getExternalFilesDirs(dirType)) match {
    case Some(dirs) if dirs.size > 1 => dirs(1)
    case _ => null
  }

  override protected def storageState = Option(podcastDirectory) match {
    case Some(dir) => Environment.getStorageState(dir)
    case None => ""
  }
}

private[storage] class LegacySecondaryExternalStorageProvider(protected val context: Context, rootDir: Option[File]) extends ExternalStorageProviderBase {
  private lazy val baseDir = rootDir.map(new File(_, "Android/mobi.upod.app/files"))

  val id = StorageProvider.ExternalSecondary

  override protected def getDirectory(dirType: String): File =
    new File(baseDir.get, dirType)

  override protected def storageState = rootDir map { root =>
    if (root.canRead && root.canWrite)
      Environment.MEDIA_MOUNTED
    else if (root.canRead)
      Environment.MEDIA_MOUNTED_READ_ONLY
    else
      ""
  } getOrElse ""
}

object StorageProvider extends Enumeration with EnumerationName with Logging {
  type StorageProviderType = Value

  val Coverart = "Coverart"
  val Podcasts = Environment.DIRECTORY_PODCASTS

  val Internal = Value(0, "Internal")
  val External = Value(1, "External")
  val ExternalSecondary = Value(2, "ExternalSecondary")

  private val secondayExternalStorageProviderFactory = if (ApiLevel >= ApiLevel.KitKat)
    new KitkatSecondaryExternalStorageProviderFactory
  else
    new LegacySecondaryExternalStorageProviderFactory

  def apply(context: Context, which: StorageProviderType): mobi.upod.app.storage.StorageProvider = which match {
    case Internal => new InternalStorageProvider(context)
    case External => new ExternalStorageProvider(context)
    case ExternalSecondary => secondayExternalStorageProviderFactory.create(context)
  }

  def getAvailableStorageTypes(context: Context): Seq[StorageProviderType] = {
    val externalDirs = ContextCompat.getExternalFilesDirs(context, Podcasts)
    val externalAvailable = externalDirs.size >= 1 && externalDirs(0) != null
    val externalSecondaryAvailable = secondayExternalStorageProviderFactory.isAvailable(context)
    (externalAvailable, externalSecondaryAvailable) match {
      case (false, false) => Seq(Internal)
      case (true, false) => Seq(Internal, External)
      case (false, true) => Seq(Internal, ExternalSecondary)
      case (true, true) => Seq(Internal, External, ExternalSecondary)
    }
  }

  def getBestSuitedStorageType(context: Context): StorageProviderType = {
    val availableStorageTypes = getAvailableStorageTypes(context)
    val externalStorageTypes = availableStorageTypes.filter(_ != Internal)
    val externalStorageProviders = externalStorageTypes.map(apply(context, _))
    val writableExternalStorageProviders = externalStorageProviders.filter(_.writable)
    if (writableExternalStorageProviders.isEmpty)
      Internal
    else
      writableExternalStorageProviders.maxBy(_.availableBytes).id
  }

  def hasExternalStoragePermissions(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED


  private trait SecondaryExternalStorageProviderFactory {

    def isAvailable(context: Context): Boolean
    
    def create(context: Context): mobi.upod.app.storage.StorageProvider
  }

  private class LegacySecondaryExternalStorageProviderFactory extends SecondaryExternalStorageProviderFactory {
    log.info("created legacy secondary external storage provider factory")

    private val sdCardPath: Option[File] = {
      val pathCandidate = try {
        val process = new ProcessBuilder().command("mount").redirectErrorStream(true).start()
        process.waitFor()
        val mountLines = upod.io.forCloseable(process.getInputStream) {
          inputStream =>
            Source.fromInputStream(inputStream, "UTF-8").getLines().toIndexedSeq
        }
        log.debug(s"found mount points:\n${mountLines.mkString("\n")}")

        val candidatePattern = "(?i).*vold.*(vfat|ntfs|exfat|fat32|ext3|ext4).*rw.*"
        val candidates = mountLines map { line =>
          if (!line.toLowerCase.contains("asec") && line.matches(candidatePattern)) {
            val parts = line.split(" ")
            val path = parts map {
              part =>
                if (part.startsWith("/") && !part.toLowerCase.contains("vold")) {
                  log.info(s"mount point $part is SD card candidate")
                  Some(part)
                } else {
                  None
                }
            } collectFirst { case Some(p) => p }
            path
          } else {
            None
          }
        }
        candidates collectFirst { case Some(p) => p }
      } catch {
        case ex: Throwable =>
          log.error("failed to read mount list")
          None
      }
      pathCandidate.map(new File(_))
    }

    override def isAvailable(context: Context): Boolean = sdCardPath.isDefined

    override def create(context: Context): StorageProvider = 
      new LegacySecondaryExternalStorageProvider(context, sdCardPath)
  }

  private class KitkatSecondaryExternalStorageProviderFactory extends SecondaryExternalStorageProviderFactory {
    log.info("created KitKat secondary external storage provider factory")

    override def create(context: Context): StorageProvider =
      new SecondaryExternalStorageProvider(context)

    override def isAvailable(context: Context): Boolean = {
      val externalDirs = ContextCompat.getExternalFilesDirs(context, Podcasts)
      externalDirs.size >= 2 && externalDirs(1) != null
    }
  }
}