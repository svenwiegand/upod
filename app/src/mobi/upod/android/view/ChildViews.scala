package mobi.upod.android.view

import android.view.{View, ViewGroup}
import android.widget._

import scala.reflect.ClassTag

trait ChildViews extends Any {

  def findViewById(id: Int): View

  def childAs[A <: View](id: Int)(implicit classTag: ClassTag[A]): A = findViewById(id) match {
    case null => throw new NoSuchElementException(s"Cannot find view with ID $id")
    case view: A => view
    case view => throw new NoSuchElementException(s"View with ID $id is of type ${view.getClass}, but expected type was ${classTag.runtimeClass}")
  }

  def optionalChildAs[A <: View](id: Int)(implicit classTag: ClassTag[A]): Option[A] = findViewById(id) match {
    case view: A => Some(view)
    case _ => None
  }

  def childView(id: Int) = childAs[View](id)

  def childButton(id: Int) = childAs[Button](id)

  def childToggleButton(id: Int) = childAs[ToggleButton](id)

  def childCheckBox(id: Int) = childAs[CheckBox](id)

  def childTextView(id: Int) = childAs[TextView](id)

  def childImageView(id: Int) = childAs[ImageView](id)

  def childProgressBar(id: Int) = childAs[ProgressBar](id)

  def childSeekBar(id: Int) = childAs[SeekBar](id)

  def childGridView(id: Int) = childAs[GridView](id)

  def childViewGroup(id: Int) = childAs[ViewGroup](id)

  implicit def viewToRichView(view: View) = new Helpers.RichView(view)
}
