package mobi.upod.app.gui.info

import android.content.Context
import com.crashlytics.android.answers.{CustomEvent, Answers}
import com.escalatesoft.subcut.inject.{BindingModule, Injectable}
import com.github.nscala_time.time.Imports._
import mobi.upod.android.app.action.Action
import mobi.upod.android.content.preferences.{BooleanPreference, DateTimePreference, Setter}
import mobi.upod.android.view.cards.{CardHeader, CardHeaders, SimpleCardHeader}
import mobi.upod.android.widget.card.{CardButton, CardView, TextCardView}
import mobi.upod.app.R
import mobi.upod.app.services.licensing.OpenGooglePlayLicenseAction
import mobi.upod.app.storage.InternalAppPreferences

trait SponsorRequestCardHeaders extends CardHeaders with Injectable {
  type BoolPref = BooleanPreference with Setter[Boolean]
  type DateTimePref = DateTimePreference with Setter[DateTime]

  private val prefs = inject[InternalAppPreferences]

  override def createCardHeaders: Seq[CardHeader] = super.createCardHeaders :+
    createCardHeader(prefs.mayShowRateRequest, prefs.lastRateRequest, R.string.rate_request_title, R.string.rate_request_details, R.string.action_rate, RateAction) :+
    createCardHeader(prefs.mayShowPurchaseRequest, prefs.lastPurchaseRequest, R.string.purchase_request_title, R.string.purchase_request_details, R.string.purchase_request_more, new OpenGooglePlayLicenseAction)

  private def createCardHeader(
    mayShow: BoolPref,
    lastShown: DateTimePref,
    titleId: Int,
    detailsId: Int,
    actionTextId: Int,
    action: Action): CardHeader = {

    new SimpleCardHeader(
      shouldShow(action, mayShow, lastShown),
      createCard(_, mayShow, lastShown, titleId, detailsId, actionTextId, action)
    )
  }
  
  private def createCard(
    context: Context,
    mayShow: BoolPref,
    lastShown: DateTimePref,
    titleId: Int,
    detailsId: Int,
    actionTextId: Int,
    action: Action): CardView = {

    implicit val ctx = context
    new TextCardView(
      ctx,
      titleId,
      detailsId,
      CardButton.primary(actionTextId, action),
      CardButton(R.string.sponsor_request_dismiss_temporary, Action(_ => dismissTemporary(mayShow, lastShown))),
      CardButton(R.string.sponsor_request_dismiss_permanent, Action(_ => dismissPermanent(mayShow, lastShown)))
    )
  }

  def shouldShow(action: Action, allowedToShow: Boolean, lastShown: DateTime): Boolean = {
    def isDue: Boolean =
      lastShown + SponsorRequestCardHeaders.RequestInterval <= DateTime.now

    allowedToShow && action.isEnabled(context) && isDue
  }

  private def dismissTemporary(
    mayShow: BooleanPreference with Setter[Boolean],
    lastShown: DateTimePreference with Setter[DateTime]): Unit = {

    mayShow := true
    lastShown := DateTime.now
  }

  private def dismissPermanent(
    mayShow: BooleanPreference with Setter[Boolean],
    lastShown: DateTimePreference with Setter[DateTime]): Unit = {

    mayShow := false
    lastShown := DateTime.now
  }

  private object RateAction extends RateAction {

    override def onFired(context: Context): Unit = {
      super.onFired(context)
      inject[Answers].logCustom(new CustomEvent("Rate App"))
      dismissPermanent(prefs.mayShowRateRequest, prefs.lastRateRequest)
    }
  }
}

object SponsorRequestCardHeaders {
  val RequestInterval = 9.days
  val RequestGap = 4.days

  def reset()(implicit bindings: BindingModule): Unit = {
    val prefs = bindings.inject[InternalAppPreferences](None)
    
    prefs.mayShowRateRequest := true
    prefs.lastRateRequest := prefs.installationDate
    
    prefs.mayShowPurchaseRequest := true
    prefs.lastPurchaseRequest := prefs.installationDate + RequestGap
  }
}