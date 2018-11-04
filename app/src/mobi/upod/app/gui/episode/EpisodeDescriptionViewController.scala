package mobi.upod.app.gui.episode

import android.app.Activity
import android.graphics.Color
import android.webkit.WebView
import mobi.upod.android.content.Theme._
import mobi.upod.android.os.AsyncTask
import mobi.upod.android.util.HtmlText.HtmlText
import mobi.upod.app.R
import mobi.upod.app.data.Episode

trait EpisodeDescriptionViewController {

  private lazy val descriptionView = findDescriptionView

  def getActivity: Activity

  protected def findDescriptionView: WebView

  protected def initDescriptionView(): Unit =
    descriptionView.setBackgroundColor(Color.argb(1, 0, 0, 0))

  protected def asyncUpdateDescriptionView(episode: => Option[Episode]): Unit =
    AsyncTask.execute(episode)(e => updateDescriptionView(e))

  protected def updateDescriptionView(episode: Option[Episode]): Unit = episode.foreach { e =>
    if (getActivity != null) {
      val baseUrl = e.link.orNull
      val description = e.description.getOrElse("")
      val html = description.htmlWithStyle("css/episode-description.css")(
        "background-color" -> getActivity.getThemeColor(R.attr.windowBackground).css)
      descriptionView.loadDataWithBaseURL(baseUrl, html, null, "utf-8", null)
    }
  }
}
