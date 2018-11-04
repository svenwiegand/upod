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

class AudioFxDialogFragment  extends DialogFragment with SeekBar.OnSeekBarChangeListener with AppInjection {
  import AudioFxDialogFragment._

  private val playbackService = inject[PlaybackService]
  private val originalGain = playbackService.volumeGain
  private var currentGainView: TextView = null
  private var currentGain = 1.0f
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
    val view = inflater.inflate(R.layout.audio_fx_control, null)

    val current = playbackService.volumeGain
    view.childTextView(R.id.audioGainMin).setText(f"$MinGain%1.1f")
    view.childTextView(R.id.audioGainMax).setText(f"$MaxGain%1.1f")
    currentGainView = view.childTextView(R.id.audioGain)
    currentGainView.setText(f"$current%1.1f dB")

    val seekBar = view.childSeekBar(R.id.audioGainControl)
    seekBar.setMax((10 * (MaxGain - MinGain)).toInt)
    seekBar.setProgress((10 * (current - MinGain)).toInt)
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
      setTitle(R.string.volume_gain).
      setView(view).
      setPositiveButton(R.string.ok, DialogClickListener(applyChanges())).
      setNegativeButton(R.string.cancel, null).
      create
  }

  private def applyChanges(): Unit = {
    applied = true
    AsyncTransactionTask.execute {
      if (applyToPodcast) {
        if (currentGain > 0)
          inject[PodcastDao].updateVolumeGain(podcastId, currentGain)
        else
          inject[PodcastDao].resetVolumeGain(podcastId)
        inject[EpisodeDao].resetVolumeGain(podcastId)
        inject[SyncService].pushSyncRequired()
      } else {
        inject[EpisodeDao].updateVolumeGain(episodeId, currentGain)
      }
    }
  }

  override def onDismiss(dialog: DialogInterface): Unit = {
    super.onDismiss(dialog)
    if (!applied) {
      playbackService.setVolumeGain(originalGain)
    }
  }


  //
  // SeekBar listener
  //

  def onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean): Unit = {
    currentGain = MinGain + progress.toFloat / 10
    currentGainView.setText(f"$currentGain%1.1f dB")
    playbackService.setVolumeGain(currentGain)
  }

  def onStartTrackingTouch(seekBar: SeekBar): Unit = {}

  def onStopTrackingTouch(seekBar: SeekBar): Unit = {}
}

object AudioFxDialogFragment {
  private val MinGain = 0f
  private val MaxGain = 6f
  private val EpisodeId = "episodeId"
  private val PodcastId = "podcastId"

  def show(activity: Activity, episode: EpisodeBaseWithPlaybackInfo): Unit = {
    val dialog = new AudioFxDialogFragment
    dialog.prepare(episode)
    dialog.show(activity.getFragmentManager, "audioFxDialog")
  }
}
