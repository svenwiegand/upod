package mobi.upod.android.view.wizard

import android.content.Context
import android.text.Html
import android.view.{LayoutInflater, ViewGroup}
import android.widget._
import mobi.upod.app.R

abstract class WizardChoicePage[A](
    key: String,
    headerId: Int,
    introductionId: Int,
    tipId: Int)
  extends WizardPage(key, headerId) {

  def choices: IndexedSeq[Choice[A]]

  protected def createChoiceGroup(context: Context): LinearLayout

  protected def createChoiceButton(choice: Choice[A], inflater: LayoutInflater, choiceGroup: LinearLayout): CompoundButton

  protected def onChoiceGroupCreated(choiceGroup: LinearLayout): Unit = {}

  override protected def createContentView(context: Context, container: ViewGroup, inflater: LayoutInflater) = {
    val choiceGroup = createChoiceGroup(context)
    val introduction = new TextView(context)
    introduction.setTextAppearance(context, R.style.TextAppearance_Wizard)
    introduction.setText(Html.fromHtml(context.getString(introductionId)))
    choiceGroup.addView(introduction)

    choices.zipWithIndex.foreach { case (choice, index) =>
      val choiceButton = createChoiceButton(choice, inflater, choiceGroup)
      choiceButton.setId(index)
      choiceButton.setText(choice.label(context))
      choiceGroup.addView(choiceButton)
    }

    if (tipId != 0) {
      val tip = new TextView(context)
      tip.setTextAppearance(context, R.style.TextAppearance_Wizard)
      tip.setText(Html.fromHtml(context.getString(tipId)))
      choiceGroup.addView(tip)
    }

    container.addView(choiceGroup)
    onChoiceGroupCreated(choiceGroup)
    choiceGroup
  }
}
