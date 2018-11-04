package mobi.upod.android.job

import android.content.Context
import com.evernote.android.job.JobRequest
import com.evernote.android.job.JobRequest.NetworkType
import mobi.upod.app.services.net.{ConnectionState, ConnectionStateRetriever}
import mobi.upod.util.Duration.LongDuration

object ConnectedJobRequestBuilder {
  val MinDelay = 100
  val MaxDelay = 1.minute.millis
  val MaxImmediateDelay = 1.seconds.millis

  def apply(tag: String, requiresNonMeteredConnection: Boolean, replaceCurrent: Boolean = false): JobRequest.Builder = new JobRequest.Builder(tag).
    setExecutionWindow(MinDelay, MaxDelay).
    setRequiredNetworkType(if (requiresNonMeteredConnection) NetworkType.UNMETERED else NetworkType.CONNECTED).
    setRequirementsEnforced(true).
    setUpdateCurrent(replaceCurrent)

  def schedule(tag: String, requiresNonMeteredConnection: Boolean, replaceCurrent: Boolean = false): Int =
    ConnectedJobRequestBuilder(tag, requiresNonMeteredConnection, replaceCurrent).build.schedule()

  def immediate(context: Context, tag: String, requiresNonMeteredConnection: Boolean, replaceCurrent: Boolean = false): JobRequest.Builder = {
    val jobRequestBuilder = ConnectedJobRequestBuilder(tag, requiresNonMeteredConnection, replaceCurrent)

    def immediateRequestBuilder: JobRequest.Builder =
      jobRequestBuilder.setExecutionWindow(MinDelay, MaxImmediateDelay)

    new ConnectionStateRetriever(context).getConnectionState match {
      case ConnectionState.Full => immediateRequestBuilder
      case ConnectionState.Metered if !requiresNonMeteredConnection => immediateRequestBuilder
      case _ => jobRequestBuilder
    }
  }

  def scheduleImmediate(context: Context, tag: String, requiresNonMeteredConnection: Boolean, replaceCurrent: Boolean = false): Int =
    immediate(context: Context, tag, requiresNonMeteredConnection, replaceCurrent).build.schedule()
}
