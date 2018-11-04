package mobi.upod.app.gui.playback

import android.content.{Context, Intent}
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.ActionBarActivity
import com.escalatesoft.subcut.inject.BindingModule
import mobi.upod.android.app.action.Action
import mobi.upod.android.app.{SimpleAlertDialogFragment, AlertDialogListener}
import mobi.upod.android.content.IntentHelpers.RichIntent
import mobi.upod.android.logging.SendDiagnosticsAction
import mobi.upod.android.os.BundleSerializableValue
import mobi.upod.app.data.EpisodeBaseWithDownloadInfo
import mobi.upod.app.services.playback.PlaybackError
import mobi.upod.app.storage.StoragePreferences
import mobi.upod.app.{AppInjection, IntentExtraKey, R}

class PlaybackErrorActivity extends ActionBarActivity with AlertDialogListener with AppInjection {

  lazy val playbackError = getIntent.getExtra(PlaybackErrorActivity.PlaybackErrorExtra)
  lazy val mediaFile = getIntent.getExtra(PlaybackErrorActivity.MediaFileExtra)
  lazy val playWithExternalPlayerAction: (Option[Int], Option[PlayWithExternalPlayerAction]) = mediaFile match {
    case Some(mf) => Some(R.string.play_with) -> Some(new PlayWithExternalPlayerAction(mf.dataUri, mf.mimeType))
    case _ => None -> None
  }

  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)

    playbackError.foreach(showErrorDialog)
  }

  private def showErrorDialog(error: PlaybackError): Unit = {

    val (errorMsgId, (positiveButton: Option[Int], positiveAction: Option[Action])) = error.reason match {
      case PlaybackError.StorageNotAvailable =>
        (R.string.playback_error_storage_not_available, None -> None)
      case PlaybackError.FileDoesNotExist =>
        (R.string.playback_error_file_does_not_exist, None -> None)
      case PlaybackError.UnsupportedFormat =>
        (R.string.playback_error_unsupported_format, playWithExternalPlayerAction)
      case PlaybackError.RemoteError =>
        (R.string.playback_error_remote, None -> None)
      case _ =>
        (R.string.playback_error_unknown, playWithExternalPlayerAction)
    }

    SimpleAlertDialogFragment.showFromActivity(
      this,
      SimpleAlertDialogFragment.defaultTag,
      R.string.playback_error,
      getString(errorMsgId),
      neutralButtonTextId = Some(R.string.close),
      positiveButtonTextId = positiveButton,
      positiveAction = positiveAction
    )
  }

  override def onAlertDialogDismissed(dialogTag: String): Unit =
    finish()
}

object PlaybackErrorActivity {

  case class MediaFileInfo(dataUri: String, mimeType: String) extends Serializable

  val PlaybackErrorExtra = new BundleSerializableValue[PlaybackError](IntentExtraKey("playbackError"))
  val MediaFileExtra = new BundleSerializableValue[MediaFileInfo](IntentExtraKey("mediaFileInfo"))

  def intent(context: Context, playbackError: PlaybackError, episode: Option[EpisodeBaseWithDownloadInfo])(implicit bindingModule: BindingModule): Intent = {
    val mediaFile = episode flatMap { e =>
      val file = bindingModule.inject[StoragePreferences](None).storageProvider.whenReadable(e.mediaFile)
      file.map(f => MediaFileInfo(Uri.fromFile(f).toString, e.media.mimeType.toString))
    }

    val intent = new Intent(context, classOf[PlaybackErrorActivity])
    intent.putExtra(PlaybackErrorExtra, playbackError)
    intent.putExtra(MediaFileExtra, mediaFile)
    intent
  }

  def start(context: Context, playbackError: PlaybackError, episode: Option[EpisodeBaseWithDownloadInfo])(implicit bindingModule: BindingModule): Unit =
    context.startActivity(intent(context, playbackError, episode))
}