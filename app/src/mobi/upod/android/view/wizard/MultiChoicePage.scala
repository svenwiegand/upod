package mobi.upod.android.view.wizard

import android.content.Context
import android.view.LayoutInflater
import android.widget._
import mobi.upod.android.view.CompoundButtonCheckedChangeListener
import mobi.upod.app.R

abstract class MultiChoicePage[A](
    key: String,
    headerId: Int,
    introductionId: Int,
    tipId: Int)
  extends WizardChoicePage[A](key, headerId, introductionId, tipId) {

  def defaultChoices: Set[A]

  private var _checked = defaultChoices

  override protected def createChoiceGroup(context: Context): LinearLayout =
    new RadioGroup(context)

  override protected def createChoiceButton(choice: Choice[A], inflater: LayoutInflater, choiceGroup: LinearLayout): CompoundButton = {
    val checkButton = inflater.inflate(R.layout.wizard_checkbox, choiceGroup, false).asInstanceOf[CheckBox]
    checkButton.setChecked(defaultChoices.contains(choice.id))
    checkButton.setOnCheckedChangeListener(CompoundButtonCheckedChangeListener(onChecked(choice.id, _)))
    checkButton
  }

  private def onChecked(choice: A, checked: Boolean): Unit = {
    if (checked)
      onCheckedChanged(_checked + choice)
    else
      onCheckedChanged(_checked - choice)
  }

  protected def onCheckedChanged(checked: Set[A]): Unit
}

abstract class SimpleMultiChoicePage[A](
    key: String,
    headerId: Int,
    introductionId: Int,
    tipId: Int,
    default: => Set[A],
    checkedChanged: Set[A] => Unit,
    availableChoices: Choice[A]*)
  extends MultiChoicePage[A](key, headerId, introductionId, tipId) {

  override def choices = availableChoices.toIndexedSeq

  override def defaultChoices = default

  override protected def onCheckedChanged(checked: Set[A]) =
    checkedChanged(checked)
}
