package mobi.upod.android.view.wizard

import android.app.Fragment
import android.content.Context
import android.os.Bundle
import android.view.{LayoutInflater, View, ViewGroup}
import mobi.upod.android.view.{ChildViews, FragmentViewFinder}
import mobi.upod.app.R

abstract class WizardPage(val key: String, val headerId: Int) extends Fragment with ChildViews with FragmentViewFinder {

  final override def onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle) = {
    val contentContainerRoot = inflater.inflate(R.layout.wizard_page, container, false).asInstanceOf[ViewGroup]
    val contentContainer = contentContainerRoot.childViewGroup(R.id.content)
    createContentView(getActivity, contentContainer, inflater)
    contentContainerRoot
  }

  protected def createContentView(context: Context, container: ViewGroup, inflater: LayoutInflater): View

  override def onDestroyView() = {
    val contentContainer = getView.childViewGroup(R.id.content)
    val view = contentContainer.getChildAt(0)
    contentContainer.removeAllViews()
    destroyContentView(view)

    super.onDestroyView()
  }

  protected def destroyContentView(contentView: View): Unit = {}

  def shouldShow: Boolean = true

  def validationError: Option[Int] = None
}
