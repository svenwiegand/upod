package mobi.upod.android.widget.card

import android.content.Context
import android.view.View
import android.widget.{RadioButton, RadioGroup}
import mobi.upod.android.widget.TextViewWithImages
import mobi.upod.app.R

class SingleChoiceCardView(
  context: Context,
  title: String,
  textWithImages: String,
  defaultChoiceIndex: Int,
  onSave: Int => Unit,
  options: String*)
  extends StandardCardView(context, title, CardButton.primary(context.getString(R.string.ok))) {

  private lazy val choiceGroup: RadioGroup = createChoiceGroup

  override protected def createContent(): Unit = {
    addContent(R.layout.card_text).asInstanceOf[TextViewWithImages].setText(textWithImages)
    addContent(choiceGroup)
  }

  private def createChoiceGroup: RadioGroup = {
    val grp = new RadioGroup(context)
    options foreach { option =>
      View.inflate(context, R.layout.card_radio_button, grp)
      val btn = grp.getChildAt(grp.getChildCount - 1).asInstanceOf[RadioButton]
      btn.setId(grp.getChildCount - 1)
      btn.setText(option)
    }
    grp.check(defaultChoiceIndex)
    grp
  }

  override protected def onDismiss(): Unit = {
    onSave(choiceGroup.getCheckedRadioButtonId)
    super.onDismiss()
  }
}
