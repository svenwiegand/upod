package mobi.upod.android.app.action

import android.app.Fragment

trait ConfirmedActionProviderFragment extends Fragment {

  def confirmedAction(tag: String): Action
}
