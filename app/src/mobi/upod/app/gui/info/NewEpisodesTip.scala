package mobi.upod.app.gui.info

import com.escalatesoft.subcut.inject.Injectable
import mobi.upod.android.os.AsyncTask
import mobi.upod.android.view.cards.{CardHeader, CardHeaders, TipCardHeader}
import mobi.upod.app.R
import mobi.upod.app.services.EpisodeService
import mobi.upod.app.services.sync.SyncService
import mobi.upod.app.storage.UiPreferences

trait NewEpisodesTip extends CardHeaders with Injectable {

  override def createCardHeaders: Seq[CardHeader] = super.createCardHeaders :+
    TipCardHeader.textTip("new", R.string.tip_new_title, R.string.tip_new_details)
}
