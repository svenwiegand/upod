package mobi.upod.android.app

import mobi.upod.android.app.action.Action

sealed trait NavigationDrawerEntry {
  val id: Long
}

sealed trait LabeledNavigationDrawerEntry extends NavigationDrawerEntry {
  val titleId: Int
}

sealed trait LabeledNavigationDrawerEntryWithIcon extends LabeledNavigationDrawerEntry {
  val iconId: Int
}

case class NavigationItem(id: Long, titleId: Int, iconId: Int, viewModes: IndexedSeq[ViewMode], counter: Int)
  extends LabeledNavigationDrawerEntryWithIcon {

  val viewModesById: Map[Int, ViewMode] =
    viewModes.map(viewMode => (viewMode.id, viewMode)).toMap

  def windowTitleId: Int = titleId

  def withUpdatedCounter(counter: Int) = copy(counter = counter)
}

object NavigationItem {
  def apply(id: Long, titleId: Int, iconId: Int, viewModes: ViewMode*) =
    new NavigationItem(id, titleId, iconId, viewModes.toIndexedSeq, 0)
}

case class NavigationActionItem(id: Long, titleId: Int, iconId: Int, action: Action) extends LabeledNavigationDrawerEntryWithIcon

case class NavigationSectionHeader(id: Long, titleId: Int) extends LabeledNavigationDrawerEntry

case class NavigationSeparator(id: Long) extends NavigationDrawerEntry