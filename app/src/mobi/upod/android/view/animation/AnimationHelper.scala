package mobi.upod.android.view.animation

import android.view.View
import android.view.animation.{AnimationUtils, Animation}

object AnimationHelper {

  implicit class RichAnimation(val animation: Animation) extends AnyVal {

    def start(view: View, onFinished: => Unit): Unit = {
      animation.setAnimationListener(AnimationListener.onEnd(onFinished))
      view.startAnimation(animation)
    }
  }

  def animate(view: View, animId: Int, onFinished: => Unit): Unit = {
    val animation = AnimationUtils.loadAnimation(view.getContext, animId)
    animation.start(view, onFinished)
  }
}