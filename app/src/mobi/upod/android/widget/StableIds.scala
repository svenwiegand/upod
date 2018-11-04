package mobi.upod.android.widget

import android.widget.BaseAdapter

trait StableIds extends BaseAdapter {

  protected def itemId(position: Int): Long

  override val hasStableIds: Boolean = true

  def getItemId(position: Int): Long = itemId(position)
}
