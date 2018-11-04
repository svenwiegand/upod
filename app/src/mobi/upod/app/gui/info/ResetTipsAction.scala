package mobi.upod.app.gui.info

import android.content.Context
import com.escalatesoft.subcut.inject.{BindingModule, Injectable}
import mobi.upod.android.app.action.Action
import mobi.upod.android.view.cards.TipCardHeader
import mobi.upod.android.widget.Toast
import mobi.upod.app.R

class ResetTipsAction(implicit val bindingModule: BindingModule) extends Action with Injectable {

  override def onFired(context: Context): Unit = {
    TipCardHeader.resetTipStatus()
    SponsorRequestCardHeaders.reset()
    Toast.show(context, R.string.pref_reset_tips_confirmation)
  }
}
