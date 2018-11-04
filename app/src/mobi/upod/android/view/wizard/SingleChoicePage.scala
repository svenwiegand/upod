package mobi.upod.android.view.wizard

import android.content.Context
import android.view.LayoutInflater
import android.widget._
import mobi.upod.app.R
import android.widget.RadioGroup.OnCheckedChangeListener

abstract class SingleChoicePage[A](
    key: String,
    headerId: Int,
    introductionId: Int,
    tipId: Int)
  extends WizardChoicePage[A](key, headerId, introductionId, tipId)
  with OnCheckedChangeListener {

  def defaultChoice: Option[A]

  def choices: IndexedSeq[Choice[A]]

  override protected def createChoiceGroup(context: Context): LinearLayout =
    new RadioGroup(context)

  override protected def createChoiceButton(choice: Choice[A], inflater: LayoutInflater, choiceGroup: LinearLayout): CompoundButton =
    inflater.inflate(R.layout.wizard_radio_button, choiceGroup, false).asInstanceOf[RadioButton]

  override protected def onChoiceGroupCreated(choiceGroup: LinearLayout): Unit = {
    val radioGroup = choiceGroup.asInstanceOf[RadioGroup]
    radioGroup.setOnCheckedChangeListener(this)
    defaultChoice.foreach(c => radioGroup.check(choices.indexWhere(_.id == c)))
  }

  override def onCheckedChanged(group: RadioGroup, checkedId: Int): Unit =
    onChoiceChanged(choices(checkedId).id)

  protected def onChoiceChanged(choice: A): Unit
}

abstract class SimpleSingleChoicePage[A](
    key: String,
    headerId: Int,
    introductionId: Int,
    tipId: Int,
    default: => Option[A],
    choiceChanged: A => Unit,
    availableChoices: Choice[A]*)
  extends SingleChoicePage[A](key, headerId, introductionId, tipId) {

  private var currentChoice: Option[A] = default

  override protected def onChoiceChanged(choice: A) = {
    choiceChanged(choice)
    currentChoice = Some(choice)
  }

  override def choices = availableChoices.toIndexedSeq

  override def defaultChoice = default

  override def validationError: Option[Int] = currentChoice match {
    case Some(_) => None
    case None => Some(R.string.wizard_single_choice_required)
  }
}
