package mobi.upod.android.util

import android.util.SparseBooleanArray
import scala.collection.mutable

object CollectionConverters {

  implicit def sparseBooleanArrayToIndexSeq(array: SparseBooleanArray): Seq[Int] = {
    val indices = mutable.ListBuffer[Int]()
    for (i <- 0 until array.size) {
      if (array.valueAt(i))
        indices += array.keyAt(i)
    }
    indices
  }
}
