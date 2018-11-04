package mobi.upod.android.widget

import android.content.Context
import android.util.AttributeSet
import android.widget.ProgressBar

class HorizontalTintableProgressBar(context: Context, attrs: AttributeSet) extends ProgressBar(context, attrs) with TintableProgressBar {
  override protected def horizontal = true
}