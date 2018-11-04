package mobi.upod.app.gui.preference

import android.app.Activity
import android.content.Context
import mobi.upod.android.app.{AbstractAlertDialogFragment, SimpleDialogFragmentObjectWithShowMethod, WaitDialogFragment}
import mobi.upod.android.logging.Logging
import mobi.upod.android.os.AsyncTask
import mobi.upod.app.services.download.DownloadService
import mobi.upod.app.storage.StorageProvider
import mobi.upod.app.storage.StorageProvider.StorageProviderType
import mobi.upod.app.{AppInjection, R}
import mobi.upod.io.RichFile

class SwitchStorageRequest(oldStorageTypeId: Int, newStorageTypeId: Int, val storageFragmentId: Int)
  extends Serializable {

  def oldStorageType: StorageProviderType =
    StorageProvider(oldStorageTypeId)

  def newStorageType: StorageProviderType =
    StorageProvider(newStorageTypeId)

  def applyNewStorage(context: Context): Unit = {
    context match {
      case activity: Activity =>
        val storageFragment = activity.getFragmentManager.findFragmentById(storageFragmentId).asInstanceOf[StoragePreferenceFragment]
        storageFragment.setStorage(newStorageType)
      case _ => // ignore
    }
  }
}

class SwitchStorageConfirmationDialogFragment
  extends AbstractAlertDialogFragment[SwitchStorageRequest](
  R.string.pref_storage_change_title,
  R.string.pref_storage_change_confirmation,
  positiveButtonTextId = Some(R.string.yes),
  neutralButtonTextId = Some(R.string.no),
  negativeButtonTextId = Some(R.string.pref_storage_change_without_deletion)
) with AppInjection with Logging {
  
  override protected def onPositiveButtonClicked(): Unit = {
    val storageRequest = dialogData
    storageRequest.applyNewStorage(getActivity)
    switchStorageWithDelete(storageRequest.oldStorageType, storageRequest.newStorageType)
  }

  override protected def onNegativeButtonClicked(): Unit =
    dialogData.applyNewStorage(getActivity)

  private def switchStorageWithDelete(oldStorageType: StorageProviderType, newStorageType: StorageProviderType): Unit = {

    def adjustData(oldStorageProvider: StorageProvider, newStorageProvider: StorageProvider): Unit = {

      def tryAndLog(msg: String, block: => Unit): Unit = {
        log.info(s"trying to $msg")
        try block catch {
          case ex: Throwable =>
            log.error(s"failed to $msg", ex)
        }
      }

      def deleteDownloads(): Unit =
        tryAndLog(s"delete downloads from $oldStorageType", inject[DownloadService].deleteAllDownloads(oldStorageProvider))

      def moveCoverart(): Unit = {
        tryAndLog(s"copy coverart files from $oldStorageType to $newStorageType", oldStorageProvider.coverartDirectory.copyRecursive(newStorageProvider.coverartDirectory))
        tryAndLog(s"delete coverart files from $oldStorageType", oldStorageProvider.coverartDirectory.deleteRecursive())
      }

      deleteDownloads()
      moveCoverart()
    }

    WaitDialogFragment.show(getActivity, R.string.wait_please)
    val activity = getActivity
    val oldStorageProvider = StorageProvider(activity, oldStorageType)
    val newStorageProvider = StorageProvider(activity, newStorageType)
    AsyncTask.execute[Unit](adjustData(oldStorageProvider, newStorageProvider)) { _ =>
      WaitDialogFragment.dismiss(activity)
    }
  }
}

object SwitchStorageConfirmationDialogFragment extends
  SimpleDialogFragmentObjectWithShowMethod[SwitchStorageRequest, SwitchStorageConfirmationDialogFragment](
    new SwitchStorageConfirmationDialogFragment, "switchStorageConfirmation")