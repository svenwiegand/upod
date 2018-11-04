package mobi.upod.app.gui.playback

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.widget.Button
import mobi.upod.android.view.Tintable
import mobi.upod.app.R

class VolumeGainIndicator(context: Context, attrs: AttributeSet)
  extends Button(context, attrs)
  with Tintable
  with ActivatableIndicator {

  private val inactiveAlpha = 0.5f
  private val activeAlpha = 1.0f
  private val enabledColor = 0xffffffff
  private val disabledColor = 0xffffffff
  private var _active = false
  private var _tint = 0xffffffff
  private var _gain = 0f

  updateText()
  setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL)
  setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_action_volume_gain, 0, 0, 0)
  setTint(_tint)

  override def setTint(color: Int): Unit = {
    _tint = color
    updateTint()
    setTextColor(_tint)
  }

  def setActive(active: Boolean): Unit = {
    _active = active
    updateTint()
  }

  def setGain(gain: Float): Unit = {
    _gain = gain
    updateTint()
    updateText()
  }

  private def updateTint(): Unit = {
    val tintColor = if (_active) {
      if (_gain > 0) _tint else enabledColor
    } else {
      disabledColor
    }
    Tintable.tint(this, tintColor)
    val alpha = if (_gain > 0) activeAlpha else inactiveAlpha
    setAlpha(alpha)
  }

  private def updateText(): Unit =
    setText(formattedText)

  private def formattedText: String =
    f"+${_gain}%1.1fdB"
}
