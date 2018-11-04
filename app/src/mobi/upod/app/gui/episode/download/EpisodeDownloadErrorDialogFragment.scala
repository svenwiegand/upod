package mobi.upod.app.gui.episode.download

import mobi.upod.android.app.{AbstractAlertDialogFragment, SimpleDialogFragmentObjectWithShowMethod}
import mobi.upod.app.{AppInjection, R}
import mobi.upod.app.data.EpisodeListItem

final class EpisodeDownloadErrorDialogFragment extends AbstractAlertDialogFragment[EpisodeListItem](
    titleId = R.string.download_error_title,
    positiveButtonTextId = Some(R.string.retry),
    neutralButtonTextId = Some(R.string.close)
  ) with AppInjection {

  override protected def message: CharSequence = getActivity.getString(
    R.string.download_error,
    int2Integer(dialogData.downloadInfo.attempts),
    dialogData.media.url,
    dialogData.downloadInfo.lastErrorText.getOrElse("?")
  )

  override protected def onPositiveButtonClicked(): Unit =
    new DownloadEpisodeAction(Some(dialogData)).fire(getActivity)
}

object EpisodeDownloadErrorDialogFragment
  extends SimpleDialogFragmentObjectWithShowMethod[EpisodeListItem, EpisodeDownloadErrorDialogFragment](new EpisodeDownloadErrorDialogFragment, "episodeDownloadErrorDialog")