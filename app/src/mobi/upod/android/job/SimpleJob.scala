package mobi.upod.android.job

import com.evernote.android.job.Job
import com.evernote.android.job.Job.{Params, Result}

object SimpleJob {

  def apply(operation: => Unit): Job = withResult {
    operation
    Result.SUCCESS
  }

  def withResult(operation: => Result): Job = new Job {
    override def onRunJob(params: Params): Result =
      operation
  }
}
