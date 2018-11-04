package mobi.upod.android.view.wizard

import android.content.Context
import android.graphics.Color
import android.view.{LayoutInflater, View, ViewGroup}
import android.webkit.WebView
import mobi.upod.android.util.HtmlText.HtmlText

abstract class WizardWebPage(key: String, headerId: Int, contentId: Int) extends WizardPage(key, headerId) {

  override protected def createContentView(context: Context, container: ViewGroup, inflater: LayoutInflater) = {
    val webView = new WebView(context)
    container.addView(webView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    webView.getSettings.setTextZoom(100)
    webView.setBackgroundColor(Color.argb(1, 255, 255, 255))
    webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null)

    val html = context.getString(contentId).htmlWithStyle("css/tips.css")()
    webView.loadDataWithBaseURL(null, html, null, "utf-8", null)
    webView
  }

  override protected def destroyContentView(contentView: View) = {
    contentView.asInstanceOf[WebView].destroy()
  }
}
