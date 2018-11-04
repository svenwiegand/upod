package mobi.upod.android.logging

import android.content.Context
import android.net.Uri
import android.os.Build
import java.io._
import java.util.Locale
import java.util.zip.{ZipEntry, ZipOutputStream}

import mobi.upod.android.content.SupportMail
import mobi.upod.app.services.net.ConnectionStateRetriever
import mobi.upod.io._

object Diagnostics {

  def logFiles(context: Context): Iterable[File] = {
    val logDir = LogConfiguration.logDir(context)
    Option(logDir.listFiles).map(_.toIterable).getOrElse(Iterable())
  }

  def writeInfoLog(context: Context): Unit = {
    val infoFile = new File(LogConfiguration.logDir(context), "device-info.txt")
    forCloseable(new FileWriter(infoFile)) { writer =>
      val packageInfo = context.getPackageManager.getPackageInfo(context.getPackageName, 0)
      writer.write(
        s"""APP
           |===
           |version code: ${packageInfo.versionCode}
           |version name: ${packageInfo.versionName}
           |
           |ANDROID
           |=======
           |release:  ${Build.VERSION.RELEASE}
           |codename: ${Build.VERSION.CODENAME}
           |sdk:      ${Build.VERSION.SDK_INT}
           |locale:   ${Locale.getDefault}
           |
           |DEVICE
           |======
           |display name: ${Build.DISPLAY}
           |manufacturer: ${Build.MANUFACTURER}
           |model:        ${Build.MODEL}
           |product:      ${Build.PRODUCT}
           |brand:        ${Build.BRAND}
           |serial:       ${Build.SERIAL}
           |tags:         ${Build.TAGS}
           |
           |NETWORK
           |=======
           |${new ConnectionStateRetriever(context).getConnectionStateString}
         """.stripMargin)
    }
  }

  def zipLogs(context: Context): File = {

    def addZipEntry(zip: ZipOutputStream, file: File): Unit = {      
      val entry = new ZipEntry(file.getName)
      zip.putNextEntry(entry)

      forCloseable(new BufferedInputStream(new FileInputStream(file))) { in =>

        def writeNextByte(): Unit = {
          val byte = in.read()
          if (byte >= 0) {
            zip.write(byte)
            writeNextByte()
          }
        }
        writeNextByte()
      }
    }

    val zipFileName = "logs.zip"
    val zipFile = new File(context.getCacheDir, zipFileName)
    zipFile.delete()
    forCloseable[FileOutputStream, Unit](new FileOutputStream(zipFile)) { fileOutput =>
      forCloseable[ZipOutputStream, Unit](new ZipOutputStream(fileOutput)) { zipOutput =>
        val files = logFiles(context)
        files.foreach(addZipEntry(zipOutput, _))
      }
    }
    zipFile.setReadable(true, false)
    zipFile
  }

  private def worldReadable(context: Context, file: File): Uri = {
    val targetDir = new File(context.getFilesDir, "sharables")
    targetDir.mkdir()
    file.copyToDir(targetDir)
    Uri.parse(s"content://mobi.upod.app.fileprovider/sharables/${file.getName}")
  }

  private def worldReadable(context: Context, files: Iterable[File]): Iterable[Uri] =
    files.map(worldReadable(context, _))

  def worldReadableLogs(context: Context): Iterable[Uri] = {
    val logs = logFiles(context)
    worldReadable(context, logs)
  }

  private def getDir(context: Context, typ: String): Option[File] =
    new File(context.getFilesDir.getParentFile, typ) match {
      case dir if dir.isDirectory => Some(dir)
      case _ => None
    }

  def worldReadablePrefs(context: Context): Iterable[Uri] = {

    def shouldInclude(fileName: String): Boolean = {
      !fileName.startsWith("_") &&
        !fileName.startsWith("com.google") &&
        !fileName.startsWith("io.fabric") &&
        !fileName.startsWith("WebView") &&
        !fileName.startsWith("TwitterAdvertising") &&
        !fileName.startsWith("com.crashlytics") &&
        !fileName.startsWith("evernote_jobs.xml")
    }

    val prefDir = getDir(context, "shared_prefs")
    val files = prefDir.map(_.listFilesRecursively.filter(f => shouldInclude(f.getName))).getOrElse(Iterable())
    worldReadable(context, files)
  }

  def worldReadableDatabases(context: Context, dbNames: Iterable[String]): Iterable[Uri] = {
    val dbDir = getDir(context, "databases")
    val files = dbDir.map(dir => dbNames.map(new File(dir, _))).getOrElse(Iterable())
    worldReadable(context, files)
  }

  def sendDiagnostics(context: Context, attachements: Iterable[Uri], subject: String = ""): Unit = {
    SupportMail.send(context, subject, "", attachements)
  }
}
