package mobi.upod.app.gui.podcast

import android.app.{Activity, Dialog, DialogFragment}
import android.content.Context
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.{LayoutInflater, View}
import android.widget.TextView
import mobi.upod.android.view.DialogClickListener
import mobi.upod.android.view.Helpers.RichView
import mobi.upod.app.{AppInjection, R}

class AddPodcastDialogFragment extends DialogFragment with AppInjection {

  private def loadView: View = {
    def setHintText(view: TextView): Unit = {
      val html = Html.fromHtml(getString(R.string.add_podcast_summary))
      view.setText(html)
      view.setMovementMethod(LinkMovementMethod.getInstance())
    }

    val inflater = getActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE).asInstanceOf[LayoutInflater]
    val view = inflater.inflate(R.layout.add_podcast_dialog, null)
    setHintText(view.childTextView(R.id.podcastUrlHint))
    view
  }

  override def onCreateDialog(savedInstanceState: Bundle): Dialog = {
    val view = loadView
    new AlertDialog.Builder(getActivity).
      setTitle(R.string.add_podcast).
      setView(view).
      setPositiveButton(
        R.string.add_podcast_button,
        DialogClickListener(addPodcastFromUrl(view.childTextView(R.id.podcastUrl).getText.toString))).
      setNeutralButton(R.string.cancel, DialogClickListener(dismiss())).
      create
  }

  private def addPodcastFromUrl(url: => String): Unit = {
    OpenPodcastUrlActivity.start(getActivity, url)
    dismiss()
  }
}

object AddPodcastDialogFragment {

  def show(activity: Activity): Unit = {
    val dialog = new AddPodcastDialogFragment
    dialog.show(activity.getFragmentManager, "addPodcastDialog")
  }
}
