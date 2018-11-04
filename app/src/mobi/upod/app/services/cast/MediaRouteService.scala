package mobi.upod.app.services.cast

import android.support.v7.media.MediaRouter.RouteInfo
import android.support.v7.media.{MediaRouteSelector, MediaRouter}
import com.escalatesoft.subcut.inject.{Injectable, BindingModule}
import com.google.android.gms.cast.{Cast, CastDevice}
import mobi.upod.android.logging.Logging
import mobi.upod.app.App
import mobi.upod.app.services.licensing.LicenseService
import mobi.upod.app.services.playback.player.MediaPlayer
import mobi.upod.app.storage.InternalAppPreferences
import mobi.upod.util.Observable

class MediaRouteService(implicit val bindingModule: BindingModule)
  extends Cast.Listener
  with Observable[MediaRouteListener]
  with Injectable
  with Logging {

  private lazy val app = inject[App]
  private lazy val preferences = inject[InternalAppPreferences]
  private lazy val licenseService = inject[LicenseService]

  private lazy val router = MediaRouter.getInstance(app.getApplicationContext)
  lazy val selector = new MediaRouteSelector.Builder().
    addControlCategory(GoogleCastDevice.CategoryGoogleCast).
    build

  private var _route: Option[MediaRouter.RouteInfo] = None
  private var _device: Option[MediaRouteDevice] = None

  //

  def currentDevice: Option[MediaRouteDevice] = _device

  def mediaPlayer: Option[MediaPlayer] = _device.flatMap(_.mediaPlayer)

  //
  // media route handling
  //

  def registerRouteChangeCallbacks(): Unit =
    router.addCallback(selector, MediaRouterCallback, MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN)

  def unregisterRouteChangeCallbacks(): Unit =
    router.removeCallback(MediaRouterCallback)

  private object MediaRouterCallback extends MediaRouter.Callback {

    override def onRouteAdded(router: MediaRouter, route: RouteInfo): Unit = {
      log.info(s"new media route available: '${route.getName}'")
      joinSessionIfApplicable(route)
    }

    override def onRouteSelected(router: MediaRouter, route: RouteInfo): Unit = {
      log.info(s"selected media route '${route.getName}'")
      connectTo(route)
    }

    override def onRouteUnselected(router: MediaRouter, route: RouteInfo): Unit = {
      log.info(s"unselected media route '${route.getName}'")
      shutdownCurrentDevice()
    }
  }

  //
  // connection handling
  //

  private def joinSessionIfApplicable(route: RouteInfo): Unit = {
    if (licenseService.isLicensed && _route.isEmpty && preferences.mediaRouteId.option.exists(_ == route.getId)) {
      def onJoin(device: Option[GoogleCastDevice]): Unit = device match {
        case Some(d) =>
          log.info("successfully joined existing session")
          _device = device
          _route = Some(route)
          preferences.mediaRouteId := route.getId
          route.select()
          fire(_.onMediaRouteDeviceConnected(d))
        case _ =>
          log.info("failed to join session")
          _device = None
          _route = None
          preferences.mediaRouteId := None
      }

      log.info("trying to join session")
      createDeviceFor(route, onJoin, true)
    }
  }

  private def createDeviceFor(route: RouteInfo, onConnected: Option[GoogleCastDevice] => Unit, requireExistingSession: Boolean = false): Unit = {
    if (route.supportsControlCategory(GoogleCastDevice.CategoryGoogleCast))
      Option(CastDevice.getFromBundle(route.getExtras)).map(device => new GoogleCastDevice(app, device, requireExistingSession, disconnectCurrentDevice, onConnected))
    else
      onConnected(None)
  }
  
  private def connectTo(route: RouteInfo): Unit = {
    if (!_route.exists(_.getId == route.getId)) {
      shutdownCurrentDevice()
      _route = Some(route)
      preferences.mediaRouteId := route.getId
      createDeviceFor(route, { device =>
        _device = device
        device.foreach(device => fire(_.onMediaRouteDeviceConnected(device)))
      })
    }
  }

  private def disconnectCurrentDevice(reconnectWhenPossible: Boolean): Unit = {
    shutdownCurrentDevice(reconnectWhenPossible)
    router.selectRoute(router.getDefaultRoute)
  }

  private def shutdownCurrentDevice(reconnectWhenPossible: Boolean = false): Unit = {
    if (_route.isDefined) {
      log.info("shutting down current device")

      _route = None
      if (!reconnectWhenPossible) {
        preferences.mediaRouteId := None
      }

      val oldDevice = _device
      _device.foreach(_.shutdown())
      _device = None
      oldDevice.foreach(device => fire(_.onMediaRouteDeviceDisconnected(device)))
    }
  }

  override protected def fireActiveState(listener: MediaRouteListener): Unit =
    _device.foreach(listener.onMediaRouteDeviceConnected)
}