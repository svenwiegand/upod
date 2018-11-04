package mobi.upod.app.gui.playback

import android.app.{Activity, Dialog, DialogFragment}
import android.content.{Context, DialogInterface}
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.view.{LayoutInflater, View}
import android.widget.CompoundButton.OnCheckedChangeListener
import android.widget.{CompoundButton, SeekBar, TextView}
import mobi.upod.android.view.DialogClickListener
import mobi.upod.android.view.Helpers.RichView
import mobi.upod.app.data.EpisodeBaseWithPlaybackInfo
import mobi.upod.app.services.playback.PlaybackService
import mobi.upod.app.services.sync.SyncService
import mobi.upod.app.storage.{AsyncTransactionTask, EpisodeDao, PodcastDao}
import mobi.upod.app.{AppInjection, R}

class PlaybackSpeedDialogFragment extends DialogFragment with SeekBar.OnSeekBarChangeListener with AppInjection {
  import PlaybackSpeedDialogFragment._

  private val playbackService = inject[PlaybackService]
  private val originalSpeed = playbackService.playbackSpeedMultiplier
  private var currentSpeedView: TextView = null
  private var currentSpeed = 1.0f
  private var applyToPodcast = false
  private var applied = false

  private def prepare(episode: EpisodeBaseWithPlaybackInfo): Unit = {
    val args = new Bundle
    args.putSerializable(EpisodeId, episode.id)
    args.putSerializable(PodcastId, episode.podcastInfo.id)
    setArguments(args)
  }

  private def episodeId: Long =
    getArguments.getLong(EpisodeId)

  private def podcastId: Long =
    getArguments.getLong(PodcastId)

  private def loadView: View = {
    val inflater = getActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE).asInstanceOf[LayoutInflater]
    val view = inflater.inflate(R.layout.playback_speed_control, null)

    val current = playbackService.playbackSpeedMultiplier
    view.childTextView(R.id.playbackSpeedMin).setText(f"$MinSpeed%1.1f")
    view.childTextView(R.id.playbackSpeedMax).setText(f"$MaxSpeed%1.1f")
    currentSpeedView = view.childTextView(R.id.playbackSpeed)
    currentSpeedView.setText(f"$current%1.1f")

    val seekBar = view.childSeekBar(R.id.playbackSpeedControl)
    seekBar.setMax((10 * (MaxSpeed - MinSpeed)).toInt)
    seekBar.setProgress((10 * (current - MinSpeed)).toInt)
    seekBar.setOnSeekBarChangeListener(this)

    view.childCheckBox(R.id.applyToPodcast).setOnCheckedChangeListener(new OnCheckedChangeListener {
      def onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean): Unit = {
        applyToPodcast = isChecked
      }
    })

    view
  }

  override def onCreateDialog(savedInstanceState: Bundle): Dialog = {
    val view = loadView
    new AlertDialog.Builder(getActivity).
      setTitle(R.string.playback_speed).
      setView(view).
      setPositiveButton(R.string.ok, DialogClickListener(applyChanges())).
      setNegativeButton(R.string.cancel, null).
      create
  }

  private def applyChanges(): Unit = {
    applied = true
    AsyncTransactionTask.execute {
      if (applyToPodcast) {
        if (currentSpeed != 1.0f)
          inject[PodcastDao].updatePlaybackSpeed(podcastId, currentSpeed)
        else
          inject[PodcastDao].resetPlaybackSpeed(podcastId)
        inject[EpisodeDao].resetPlaybackSpeed(podcastId)
        inject[SyncService].pushSyncRequired()
      } else {
        if (currentSpeed != 1.0f)
          inject[EpisodeDao].updatePlaybackSpeed(episodeId, currentSpeed)
        else
          inject[EpisodeDao].resetPlaybackSpeed(episodeId)
      }
    }
  }

  override def onDismiss(dialog: DialogInterface): Unit = {
    super.onDismiss(dialog)
    if (!applied) {
      playbackService.setPlaybackSpeedMultiplier(originalSpeed)
    }
  }


  //
  // SeekBar listener
  //

  def onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean): Unit = {
    currentSpeed = MinSpeed + progress.toFloat / 10
    currentSpeedView.setText(f"$currentSpeed%1.1f")
    playbackService.setPlaybackSpeedMultiplier(currentSpeed)
  }

  def onStartTrackingTouch(seekBar: SeekBar): Unit = {}

  def onStopTrackingTouch(seekBar: SeekBar): Unit = {}
}

object PlaybackSpeedDialogFragment {
  private val MinSpeed = 0.5f
  private val MaxSpeed = 2.5f
  private val EpisodeId = "episodeId"
  private val PodcastId = "podcastId"

  def show(activity: Activity, episode: EpisodeBaseWithPlaybackInfo): Unit = {
    val dialog = new PlaybackSpeedDialogFragment
    dialog.prepare(episode)
    dialog.show(activity.getFragmentManager, "playbackSpeedDialog")
  }
}
