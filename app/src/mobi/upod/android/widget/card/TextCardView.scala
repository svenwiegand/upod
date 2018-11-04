package mobi.upod.android.widget.card

import android.content.Context
import android.widget.TextView
import mobi.upod.android.widget.TextViewWithImages
import mobi.upod.app.R

class TextCardView(context: Context, title: String, textWithImages: String, buttons: CardButton*)
  extends StandardCardView(context, title, buttons: _*) {

  private lazy val textView: TextView = contentContainer.getChildAt(0).asInstanceOf[TextView]

  def this(context: Context, titleId: Int, textWithImagesId: Int, buttons: CardButton*) {
    this(context, context.getString(titleId), context.getString(textWithImagesId), buttons: _*)
  }

  protected def setText(text: CharSequence): Unit =
    textView.setText(text)

  override protected def createContent(): Unit =
    addContent(R.layout.card_text).asInstanceOf[TextViewWithImages].setText(textWithImages)
}
