package mobi.upod.app.services.sync.gdrive

import java.io._

import android.content.Context
import android.os.Bundle
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener
import com.google.android.gms.common.api._
import com.google.android.gms.drive._
import com.google.android.gms.drive.query.{Filters, Query, SearchableField}
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.util.ExponentialBackOff
import com.google.api.services.drive
import com.google.api.services.drive.DriveScopes
import mobi.upod
import mobi.upod.android.logging.Logging
import mobi.upod.app.R
import mobi.upod.io.CharsetName

import scala.collection.JavaConverters._

class GDriveStatusException(val status: Status)
  extends IOException(s"Operation failed with status code ${status.getStatusCode}: ${status.getStatusMessage}")

trait GDriveConnectionListener {
  def onGDriveConnectionFailed(client: GDriveClient, result: ConnectionResult): Unit = ()
  def onGDriveConnected(client: GDriveClient, connectionHint: Bundle): Unit = ()
  def onGDriveConnectionSuspended(client: GDriveClient, cause: Int): Unit = ()
}

class GDriveClient private (context: Context, connectionListener: Option[GDriveConnectionListener])
  extends GoogleApiClient.ConnectionCallbacks
  with OnConnectionFailedListener
  with Logging {

  import GDriveClient._

  private val apiClient = new GoogleApiClient.Builder(context).
    addApi(Drive.API).
    addScope(Scope).
    addConnectionCallbacks(this).
    addOnConnectionFailedListener(this).
    build
  private var _connectionStatus = ConnectionStatus.NotConnected

  def connect(): Unit =
    apiClient.connect()

  def blockingConnect() =
    apiClient.blockingConnect()

  def disconnect(): Unit =
    apiClient.disconnect()

  def connectionStatus = _connectionStatus

  override def onConnectionFailed(connectionResult: ConnectionResult): Unit = {
    log.warn(s"GDrive connection failed: ${connectionResult.getErrorMessage}")
    connectionListener.foreach(_.onGDriveConnectionFailed(this, connectionResult))
  }

  override def onConnected(connectionHint: Bundle): Unit = {
    log.info("GDrive connected")
    _connectionStatus = ConnectionStatus.Connected
    connectionListener.foreach(_.onGDriveConnected(this, connectionHint))
  }

  override def onConnectionSuspended(cause: Int): Unit = {
    log.info(s"GDrive connection suspended (cause=$cause)")
    _connectionStatus = ConnectionStatus.Suspended
    connectionListener.foreach(_.onGDriveConnectionSuspended(this, cause))
  }

  def ensureSynced(): Unit = {
    log.info("Initiating GDrive sync")
    val syncStatus = Drive.DriveApi.requestSync(apiClient).await()
    if (!syncStatus.isSuccess && syncStatus.getStatusCode != DriveStatusCodes.DRIVE_RATE_LIMIT_EXCEEDED)
      syncStatus.failOrGet

    log.info("GDrive in sync")
  }

  /** This may block so don't call it from the UI thread. */
  lazy val appFolder: DriveFolder = {
    if (IsDebug)
      getSimulatedAppFolder
    else
      Drive.DriveApi.getAppFolder(apiClient)
  }

  private def getSimulatedAppFolder: DriveFolder = {
    val FolderTitle = "upod"
    val rootFolder = Drive.DriveApi.getRootFolder(apiClient)

    def createSimulatedAppFolder(): DriveFolder = {
      log.info("creating simulated app folder")
      val metadataChangeSet = new MetadataChangeSet.Builder().setTitle(FolderTitle).build
      rootFolder.createFolder(apiClient, metadataChangeSet).await().failOrGet.getDriveFolder
    }

    log.info(s"trying to get simulated app folder")
    val appFolderQuery = new Query.Builder().
      addFilter(Filters.eq(SearchableField.TITLE, FolderTitle)).
      addFilter(Filters.eq(SearchableField.TRASHED, java.lang.Boolean.FALSE)).
      build
    rootFolder.queryChildren(apiClient, appFolderQuery).await().failOrGet.map { result =>
      result.getMetadataBuffer.iterator.asScala.find(m => m.isFolder) match {
        case Some(metadataBufferResult) => metadataBufferResult.getDriveId.asDriveFolder()
        case None => createSimulatedAppFolder()
      }
    }
  }

  def emptyAppFolder(): Unit = {
    log.info("emptying app folder")
    appFolder.listChildren(apiClient).await().failOrGet.getMetadataBuffer.forEach { metadataBuffer: MetadataBuffer =>
      metadataBuffer.iterator.asScala foreach { metadata =>
        log.info(s"deleting ${metadata.getOriginalFilename}")
        metadata.getDriveId.asDriveResource.delete(apiClient)
      }
    }
  }

  def findJsonFiles(namePattern: String): Seq[DriveFile] = {
    val fileQuery = new Query.Builder().
      addFilter(Filters.eq(SearchableField.TRASHED, java.lang.Boolean.FALSE)).
      addFilter(Filters.eq(SearchableField.MIME_TYPE, "application/json")).
      build
    appFolder.queryChildren(apiClient, fileQuery).await().failOrGet map { metadataBuffer =>
      val matchingMetadata = metadataBuffer.getMetadataBuffer.iterator.asScala filter { m =>
        !m.isFolder && m.getTitle.matches(namePattern)
      }
      matchingMetadata.map(_.getDriveId.asDriveFile()).toList
    }
  }

  def findJsonFile(name: String): Option[DriveFile] = {
    val fileQuery = new Query.Builder().
      addFilter(Filters.eq(SearchableField.TRASHED, java.lang.Boolean.FALSE)).
      addFilter(Filters.eq(SearchableField.TITLE, name)).
      addFilter(Filters.eq(SearchableField.MIME_TYPE, "application/json")).
      build
    appFolder.queryChildren(apiClient, fileQuery).await().failOrGet.map { result =>
      result.getMetadataBuffer.iterator.asScala.find(m => !m.isFolder).map(_.getDriveId.asDriveFile())
    }
  }

  def createJsonFile(name: String, write: BufferedWriter => Unit): DriveFile = {
    log.info(s"creating file $name")
    val contents = Drive.DriveApi.newDriveContents(apiClient).await().failOrGet.getDriveContents
    upod.io.forCloseable(new BufferedWriter(new OutputStreamWriter(contents.getOutputStream, CharsetName.utf8)))(write)

    val metadata = new MetadataChangeSet.Builder().setMimeType("application/json").setTitle(name).build
    appFolder.createFile(apiClient, metadata, contents).await().failOrGet.getDriveFile
  }

  def createOrUpdateJsonFile(name: String, write: BufferedWriter => Unit): Unit = {
    findJsonFile(name) match {
      case Some(file) => updateFile(file, write)
      case None => createJsonFile(name, write)
    }
  }

  def readJsonFile[A](name: String, read: BufferedReader => A): Option[A] =
    findJsonFile(name).map(readFile(_, read))

  private def openFileContent(file: DriveFile, mode: Int): DriveContents =
    file.open(apiClient, mode, null).await().failOrGet.getDriveContents

  def readFile[A](file: DriveFile, read: BufferedReader => A): A = {
    val contents = openFileContent(file, DriveFile.MODE_READ_ONLY)
    try {
      upod.io.forCloseable(new BufferedReader(new InputStreamReader(contents.getInputStream, CharsetName.utf8)))(read)
    } finally contents.discard(apiClient)
  }

  def updateFile(file: DriveFile, write: BufferedWriter => Unit): Unit = {
    val contents = openFileContent(file, DriveFile.MODE_WRITE_ONLY)
    try {
      upod.io.forCloseable(new BufferedWriter(new OutputStreamWriter(contents.getOutputStream, CharsetName.utf8)))(write)
      contents.commit(apiClient, null)
    } catch {
      case e: Throwable =>
        contents.discard(apiClient)
        throw e
    }
  }
}

object GDriveClient extends Logging {
  val IsDebug = false//todo: BuildConfig.DEBUG
  val Scope = if (IsDebug) Drive.SCOPE_FILE else Drive.SCOPE_APPFOLDER

  object ConnectionStatus extends Enumeration {
    type ConnectionStatus = Val
    val NotConnected, Connected, Suspended = Value
  }

  def apply(context: Context, connectionListener: GDriveConnectionListener): GDriveClient =
    new GDriveClient(context, Some(connectionListener))

  def apply(context: Context): GDriveClient =
    new GDriveClient(context, None)

  def fixAppFolder(context: Context, accountName: String): Unit = {
    val scopes = Array(DriveScopes.DRIVE_APPDATA)
    val credential = GoogleAccountCredential.usingOAuth2(context.getApplicationContext, scopes.toList.asJava).setBackOff(new ExponentialBackOff())
    credential.setSelectedAccountName(accountName)

    val transport = AndroidHttp.newCompatibleTransport
    val jsonFactory = JacksonFactory.getDefaultInstance
    val driveService = new drive.Drive.Builder(transport, jsonFactory, credential).
      setApplicationName(context.getString(R.string.app_name)).
      build

    val fileMetaData = new drive.model.File
    fileMetaData.setName("fixAppDataFolder").
      setMimeType("text/plain").
      setParents(List("appDataFolder").asJava)

    val appDataFile = driveService.files.
      create(fileMetaData).
      setFields("id").
      execute()

    driveService.files().delete(appDataFile.getId).execute()
  }

  private implicit class ExtendedReleasable[A <: Releasable](val releasable: A) extends AnyVal {

    def map[B](f: A => B): B = try f(releasable) finally releasable.release()

    def forEach(f: A => Unit): Unit = map(f)
  }

  private implicit class ExtendedResult[A <: Result](val result: A) extends AnyVal {

    def failOrGet: A =
      if (result.getStatus.isSuccess) result else throw new GDriveStatusException(result.getStatus)
  }
}