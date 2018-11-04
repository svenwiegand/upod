package mobi.upod.app.gui.playback

import android.app.Activity
import android.content.Context
import android.support.v7.widget.PopupMenu
import android.support.v7.widget.PopupMenu.OnMenuItemClickListener
import android.util.AttributeSet
import android.view.View.OnClickListener
import android.view.{View, Menu, MenuItem, Gravity}
import android.widget.Button
import mobi.upod.android.logging.Logging
import mobi.upod.android.os.{PowerManager, Runnable}
import mobi.upod.android.view.Tintable
import mobi.upod.app.{AppInjection, R}
import mobi.upod.app.services.playback.{PlaybackService, SleepTimerMode}
import mobi.upod.timedurationpicker.TimeDurationUtil

import scala.util.Try

final class SleepTimerController(context: Context, attrs: AttributeSet)
  extends Button(context, attrs)
  with Tintable
  with ActivatableIndicator
  with OnClickListener
  with OnMenuItemClickListener
  with AppInjection
  with Logging {

  private val playbackService = inject[PlaybackService]
  private val powerManager = inject[PowerManager]
  private val inactiveAlpha = 0.5f
  private val activeAlpha = 1.0f
  private val enabledColor = 0xffffffff
  private val disabledColor = 0xffffffff
  private var _active = false
  private var _tint = 0xffffffff
  private var _mode: SleepTimerMode = SleepTimerMode.Off
  private var _hasChapters = false
  private var _activity: Option[Activity] = None
  private var _visible = false
  private val popupMenu = new PopupMenu(getContext, this)

  init()

  private def init(): Unit = {
    updateText()
    setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL)
    setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_action_sleep_timer, 0)
    setTint(_tint)
    setOnClickListener(this)
    initMenu()
  }

  def setActivity(activity: Activity): Unit =
    _activity = Some(activity)

  override def setTint(color: Int): Unit = {
    _tint = color
    updateTint()
    setTextColor(_tint)
  }

  override def setActive(active: Boolean): Unit = {
    _active = active
    updateTint()
  }

  def setMode(mode: SleepTimerMode): Unit = {
    _mode = mode
    updateTint()
    updateText()
    invalidateMenu()
    invalidateUpdateTimer()
  }

  def setHasChapter(hasChapters: Boolean): Unit = {
    _hasChapters = hasChapters
    invalidateMenu()
  }

  private def updateTint(): Unit = {
    val tintColor = if (_active) {
      if (_mode != SleepTimerMode.Off) _tint else enabledColor
    } else {
      disabledColor
    }
    Tintable.tint(this, tintColor)
    val alpha = if (_mode != SleepTimerMode.Off) activeAlpha else inactiveAlpha
    setAlpha(alpha)
  }

  private def updateText(): Unit = {
    val text = _mode match {
      case SleepTimerMode.Chapter => getContext.getString(R.string.sleep_timer_mode_chapter)
      case SleepTimerMode.Episode => getContext.getString(R.string.sleep_timer_mode_episode)
      case t: SleepTimerMode.Timer => TimeDurationUtil.formatMinutesSeconds(t.remaining)
      case _ => ""
    }
    setText(text + " ")
  }

  def becomingVisible(): Unit = {
    _visible = true
    invalidateUpdateTimer()
  }

  def becomingInvisible(): Unit =
    _visible = false

  private def invalidateUpdateTimer(): Unit = {
    Try(updateText())
    _mode match {
      case t: SleepTimerMode.Timer if _visible => Timer.ensureTriggered()
      case _ =>
    }
  }

  private object Timer extends Runnable {
    private var triggered = false

    def ensureTriggered(): Unit = if (!triggered) {
      triggered = true
      postDelayed(this, 1000)
    }

    override def run(): Unit = {
      log.debug("received update timer event")
      triggered = false
      invalidateUpdateTimer()
    }
  }

  //
  // menu stuff
  //

  override def onClick(v: View): Unit =
    popupMenu.show()

  private def initMenu(): Unit = {
    popupMenu.setOnMenuItemClickListener(this)
    popupMenu.inflate(R.menu.sleep_timer)
    invalidateMenu()
  }

  private def invalidateMenu(): Unit = {
    val menu = popupMenu.getMenu
    menu.findItem(R.id.sleep_timer_action_chapter).setEnabled(_mode != SleepTimerMode.Chapter && _hasChapters)
    menu.findItem(R.id.sleep_timer_action_chapter).setVisible(_hasChapters)

    menu.findItem(R.id.sleep_timer_action_episode).setEnabled(_mode != SleepTimerMode.Episode)

    menu.findItem(R.id.sleep_timer_action_timer).setEnabled(!_mode.isInstanceOf[SleepTimerMode.Timer])

    menu.findItem(R.id.sleep_timer_action_off).setEnabled(_mode != SleepTimerMode.Off)
    menu.findItem(R.id.sleep_timer_action_off).setVisible(_mode != SleepTimerMode.Off)
  }

  override def onMenuItemClick(menuItem: MenuItem): Boolean = {
    menuItem.getItemId match {
      case R.id.sleep_timer_action_chapter => playbackService.startSleepTimer(SleepTimerMode.Chapter)
      case R.id.sleep_timer_action_episode => playbackService.startSleepTimer(SleepTimerMode.Episode)
      case R.id.sleep_timer_action_off => playbackService.cancelSleepTimer()
      case R.id.sleep_timer_action_timer => showTimePicker()
    }
    true
  }

  private def showTimePicker(): Unit =
    _activity.foreach(a => new SleepTimerDurationPickerFragment().show(a.getFragmentManager, "dialog"))
}