package mobi.upod.android.widget

import android.content.Context

trait SimpleHeader {
  val id: Long
  def text(context: Context): String
}

case class SimpleTextHeader(id: Long, text: String) extends SimpleHeader {

  def text(context: Context) = text
}

case class SimpleTextResourceHeader(id: Long, textId: Int) extends SimpleHeader {

  def text(context: Context) = context.getString(textId)
}

case class Separator(id: Long) extends SimpleHeader {
  override def text(context: Context): String = null
}