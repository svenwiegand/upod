package mobi.upod.app.services.net

import android.content.Context
import android.net.ConnectivityManager
import android.support.v4.net.ConnectivityManagerCompat
import mobi.upod.android.logging.Logging
import mobi.upod.app.services.net.ConnectionState._

class ConnectionStateRetriever(context: Context) extends Logging {
  private lazy val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE).asInstanceOf[ConnectivityManager]

  private def metered = ConnectivityManagerCompat.isActiveNetworkMetered(connectivityManager)

  def getConnectionStateString: String = connectivityManager.getActiveNetworkInfo match {
    case null => "not connected"
    case networkInfo =>
      s"""networkType:    ${networkInfo.getTypeName}
         |networkSubtype: ${networkInfo.getSubtypeName}
         |isConnected:    ${networkInfo.isConnected}
         |isWifi:         ${networkInfo.getType == ConnectivityManager.TYPE_WIFI}
         |isEthernet:     ${networkInfo.getType == ConnectivityManager.TYPE_ETHERNET}
         |isRoaming:      ${networkInfo.isRoaming}
         |isMetered:      $metered
        """.stripMargin
  }

  def logConnectionState(): Unit =
    log.debug(s"Network status:\n$getConnectionStateString")

  def getConnectionState: ConnectionState = {
    val available = Option(connectivityManager.getActiveNetworkInfo).exists(_.isConnected)
    if (available)
      if (metered) Metered else Full
    else
      Unconnected
  }

  def isInternetAvailable: Boolean = getConnectionState match {
    case Full | Metered => true
    case _ => false
  }

  def isMeteredConnection: Boolean =
    getConnectionState == Metered

  def isUnmeteredConnection: Boolean =
    getConnectionState == Full
}
