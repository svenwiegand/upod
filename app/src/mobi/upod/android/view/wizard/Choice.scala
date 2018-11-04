package mobi.upod.android.view.wizard

import android.content.Context
import android.text.Html

trait Choice[A] {
  def id: A
  def label(context: Context): CharSequence
}

case class ValueChoice[A](id: A, labelId: Int) extends Choice[A] {

  override def label(context: Context) =
    Html.fromHtml(context.getString(labelId))
}

case class ValueChoiceWithStringLabel[A](id: A, labelText: String) extends Choice[A] {

  override def label(context: Context) = labelText
}

case class ObjectChoice[A](id: A, label: A => CharSequence) extends Choice[A] {

  override def label(context: Context) = label(id)
}

