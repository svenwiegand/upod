package mobi.upod.android.view

import android.view.View
import android.app.Fragment

trait FragmentViewFinder extends Fragment {

  def findViewById(id: Int): View = getActivity.findViewById(id)
}
