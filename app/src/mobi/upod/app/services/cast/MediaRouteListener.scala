package mobi.upod.app.services.cast

trait MediaRouteListener {

  def onMediaRouteDeviceConnected(device: MediaRouteDevice): Unit
  
  def onMediaRouteDeviceDisconnected(device: MediaRouteDevice): Unit
}
