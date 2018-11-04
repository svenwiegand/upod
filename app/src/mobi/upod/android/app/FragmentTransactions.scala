package mobi.upod.android.app

import android.app.{Fragment, FragmentManager, FragmentTransaction}

object FragmentTransactions {

  implicit class RichFragmentManager(val manager: FragmentManager) extends AnyVal {

    def inTransaction[A](block: FragmentTransaction => A): A =
      inTransactionHelper(block, _.commit())

    def inTransactionAllowingStateLoss[A](block: FragmentTransaction => A): A =
      inTransactionHelper(block, _.commitAllowingStateLoss())

    private def inTransactionHelper[A](block: FragmentTransaction => A, commit: FragmentTransaction => Unit): A = {
      val trx = manager.beginTransaction()
      val result = block(trx)
      commit(trx)
      result
    }
  }

  implicit class RichFragmentTransaction(val trx: FragmentTransaction) extends AnyVal {

    def addTransient(containerViewId: Int, fragment: Fragment) {
      trx.add(containerViewId, fragment)
      fragment.setRetainInstance(false)
    }
  }
}
