package mobi.upod.app.gui.episode

import android.animation.{Animator, AnimatorListenerAdapter, ValueAnimator}
import android.view.{ViewGroup, View}
import android.widget.ListView
import mobi.upod.android.view.Helpers._
import mobi.upod.app.gui.ListViewCloser
import android.app.Activity

class EpisodeDismissController(adapter: => EpisodeAdapter, listView: => ListView, viewCloser: => ListViewCloser) {
  private var pendingDismisses: Set[PendingDismiss] = Set()
  private var dismissAnimationCount = 0
  private lazy val animationTime = listView.getContext.getResources.getInteger(android.R.integer.config_shortAnimTime)

  private case class PendingDismiss(episodeId: Long, view: Option[View]) {
    override def hashCode(): Int = episodeId.toInt
  }

  def dismiss(episodeId: Long) {
    val dismissView = viewByEpisodeId(episodeId)
    dismissView foreach { view =>
      performDismiss(episodeId, view)
    }
    pendingDismisses += PendingDismiss(episodeId, dismissView)
  }

  private def performDismiss(episodeId: Long, dismissView: View) {
    // Animate the dismissed list item to zero-height and fire the dismiss callback when
    // all dismissed list item animations have completed. This triggers layout on each animation
    // frame; in the future we may want to do something smarter and more performant.
    val layoutParams = dismissView.getLayoutParams
    val originalHeight = dismissView.getHeight

    val animator = ValueAnimator.ofInt(originalHeight, 1).setDuration(animationTime)
    dismissAnimationCount += 1
    animator.addListener(new AnimatorListenerAdapter {
      override def onAnimationEnd(animation: Animator) {
        dismissAnimationCount -= 1
        commitPendingDismissesIfApplicable()
      }
    })
    animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener {
      def onAnimationUpdate(valueAnimator: ValueAnimator) {
        layoutParams.height = valueAnimator.getAnimatedValue.asInstanceOf[Int]
        dismissView.setLayoutParams(layoutParams)
      }
    })
    animator.start()
  }

  def commitPendingDismissesIfApplicable() {

    if (dismissAnimationCount == 0 && !listView.getContext.asInstanceOf[Activity].isFinishing) {
      adapter.remove(pendingDismisses map { _.episodeId })

      pendingDismisses filter { _.view.isDefined } map { _.view.get } foreach { view =>
        view.setAlpha(1f)
        view.setTranslationX(0)
        val lp = view.getLayoutParams
        lp.height = ViewGroup.LayoutParams.WRAP_CONTENT
        view.setLayoutParams(lp)
      }
      pendingDismisses = Set()
      if (adapter.isEmpty) {
        viewCloser.closeListView()
      }
    }
  }

  private def viewByEpisodeId(episodeId: Long): Option[View] = {
    listView.childViews find { adapter.episodeByView(_) exists { _.id == episodeId } }
  }
}
