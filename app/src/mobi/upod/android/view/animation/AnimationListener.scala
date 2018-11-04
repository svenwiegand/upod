package mobi.upod.android.view.animation

import android.view.animation.Animation

trait AnimationListener extends Animation.AnimationListener {

  override def onAnimationStart(animation: Animation): Unit = ()

  override def onAnimationEnd(animation: Animation): Unit = ()

  override def onAnimationRepeat(animation: Animation): Unit = ()
}

object AnimationListener {

  def onEnd(handle: Animation => Unit): AnimationListener = new AnimationListener {
    override def onAnimationEnd(animation: Animation): Unit = handle(animation)
  }

  def onEnd(handle: => Unit): AnimationListener = onEnd(_ => handle)
}