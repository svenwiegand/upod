package mobi.upod.app.gui

import android.app.Fragment
import android.os.Bundle
import mobi.upod.android.app.SupportActionBar
import mobi.upod.app.gui.UsageTips.ShowcaseTip
import mobi.upod.app.{AppInjection, R}

trait EpisodeListViewModeTip extends Fragment with UsageTips with SupportActionBar with AppInjection {

  override def usageTips: Seq[ShowcaseTip] = super.usageTips ++ Seq(
    UsageTips.ShowcaseTip("episode_view_mode", R.string.tip_view_mode, R.string.tip_view_mode_details, supportActionBar.getCustomView)
  )

  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)
    showUsageTips()
  }
}
