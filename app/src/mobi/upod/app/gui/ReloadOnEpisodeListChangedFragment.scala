package mobi.upod.app.gui

import mobi.upod.android.app.{ListenerFragment, SimpleReloadableFragment}
import mobi.upod.app.services.{EpisodeService, EpisodeListener}
import com.escalatesoft.subcut.inject.Injectable
import android.os.Bundle

trait ReloadOnEpisodeListChangedFragment[A]
  extends SimpleReloadableFragment[A]
  with EpisodeListener
  with Injectable {

  protected lazy val episodeService = inject[EpisodeService]

  override def onStart(): Unit = {
    super.onStart()
    episodeService.addWeakListener(this)
  }

  override def onStop(): Unit = {
    episodeService.removeListener(this)
    super.onStop()
  }

  def onEpisodeCountChanged() {
    requestReload()
  }
}
