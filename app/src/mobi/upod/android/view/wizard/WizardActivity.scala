package mobi.upod.android.view.wizard

import android.app.{Activity, Fragment}
import android.content.{Context, Intent}
import android.os.Bundle
import android.support.v13.app.FragmentPagerAdapter
import android.support.v4.view.{PagerAdapter, ViewPager}
import android.support.v4.view.ViewPager.OnPageChangeListener
import android.support.v7.app.ActionBarActivity
import android.widget.TextView
import mobi.upod.android.app.SimpleAlertDialogFragment
import mobi.upod.android.view.ChildViews
import mobi.upod.app.R

abstract class WizardActivity extends ActionBarActivity with OnPageChangeListener with ChildViews {
  private lazy val pageHeader = childAs[TextView](R.id.header)
  private lazy val pager = childAs[ViewPager](R.id.pager)
  private lazy val backButton = childView(R.id.backButton)
  private lazy val nextButton = childView(R.id.nextButton)
  private lazy val closeButton = childView(R.id.closeButton)

  protected def hasNextPage(currentPageIndex: Int, currentPageKey: String): Boolean

  protected def createFirstPage: WizardPage

  protected def createNextPage(currentPageIndex: Int, currentPageKey: String): WizardPage

  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.wizard_frame)
    pager.setOnPageChangeListener(this)
    pager.setAdapter(StepAdapter)
    backButton.onClick(goToPrevPage())
    nextButton.onClick(goToNextPage())
    closeButton.onClick(onFinishButtonClicked())
    onPageSelected(0)
  }

  override def onActivityResult(requestCode: Int, resultCode: Int, data: Intent): Unit = {
    super.onActivityResult(requestCode, resultCode, data)
    currentPage.onActivityResult(requestCode, resultCode, data)
  }

  def onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int): Unit = {}

  def onPageScrollStateChanged(state: Int): Unit = {}

  def onPageSelected(position: Int): Unit = {
    val page = currentPage
    val isFistPage = position == 0
    val isLastPage = !hasNextPage(position, page.key)

    pageHeader.setText(page.headerId)
    backButton.makeInvisible(isFistPage)
    nextButton.makeInvisible(isLastPage)
    closeButton.makeInvisible(!isLastPage)
  }

  protected def currentPage: WizardPage =
    StepAdapter.getItem(pager.getCurrentItem).asInstanceOf[WizardPage]

  private def showValidationError(msgId: Int): Unit = SimpleAlertDialogFragment.showFromActivity(
    this,
    SimpleAlertDialogFragment.defaultTag,
    0,
    getString(msgId),
    neutralButtonTextId = Some(R.string.ok)
  )

  private def validateCurrentPage(): Boolean = currentPage.validationError match {
    case Some(errorCode) =>
      showValidationError(errorCode)
      false
    case _ => true
  }


  def goToNextPage(): Unit = if (validateCurrentPage() && hasNextPage(pager.getCurrentItem, currentPage.key)) {
    StepAdapter.pushPage(createNextPage(pager.getCurrentItem, currentPage.key))
    pager.setCurrentItem(pager.getCurrentItem + 1, true)
  }

  def goToPrevPage(): Unit = if (StepAdapter.getCount > 1) {
    pager.setCurrentItem(pager.getCurrentItem - 1, true)
    StepAdapter.popPage()
  }

  def clickFinishButton(): Unit =
    onFinishButtonClicked()

  protected def onFinishButtonClicked(): Unit =
    finishWizard()

  def finishWizard(): Unit = if (validateCurrentPage()) {
    onFinish()

    val intent = new Intent(this, followUpActivity)
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK)
    finish()
    startActivity(intent)
    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
  }

  protected def followUpActivity: Class[_ <: Activity]

  protected def onFinish(): Unit

  private object StepAdapter extends FragmentPagerAdapter(getFragmentManager) {
    private var pages: List[WizardPage] = createFirstPage :: Nil

    def getCount: Int = pages.size

    def getItem(i: Int): Fragment = pages.reverse(i)

    override def getItemPosition(`object`: scala.Any): Int = pages.reverse.indexOf(`object`) match {
      case -1 =>
        PagerAdapter.POSITION_NONE
      case index =>
        index
    }

    def pushPage(nextPage: WizardPage): Unit = {
      pages = nextPage :: pages
      notifyDataSetChanged()
    }

    def popPage(): Unit = {
      getFragmentManager.beginTransaction().remove(pages.head).commit()
      pages = pages.tail
      notifyDataSetChanged()
    }
  }
}

object WizardActivity {

  def intent(context: Context, activityClass: Class[_ <: WizardActivity]): Intent =
    new Intent(context, activityClass)

  def start(activity: Activity, activityClass: Class[_ <: WizardActivity]): Unit = {
    val i = intent(activity, activityClass)
    activity.startActivity(i)
    activity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
  }
}