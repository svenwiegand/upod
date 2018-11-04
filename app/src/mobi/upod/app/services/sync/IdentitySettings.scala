package mobi.upod.app.services.sync

import com.escalatesoft.subcut.inject.BindingModule
import mobi.upod.app.storage.{PlaybackPreferences, UiPreferences, PlaybackNotificationButtons}
import mobi.upod.data.{Mapping, MappingProvider}
import mobi.upod.util.MediaPositionFormat

private[sync] case class IdentitySettings(
  hideNewInLibrary: Boolean,
  autoAddToPlaylist: Boolean,
  skipNew: Boolean,
  showMediaTypeFilter: Boolean,
  mediaTimeFormat: MediaPositionFormat.MediaPositionFormat,
  notificationButtons: PlaybackNotificationButtons.PlaybackNotificationButtons,
  fastForwardSeconds: Int,
  rewindSeconds: Int
) {

  def applyToPreferences(bindings: BindingModule): Unit = {

    def applytoUiPreferences(): Unit = {
      val p = bindings.inject[UiPreferences](None)
      p.hideNewInLibrary := hideNewInLibrary
      p.autoAddToPlaylist := autoAddToPlaylist
      p.skipNew := skipNew
      p.showMediaTypeFilter := showMediaTypeFilter
    }

    def applyToPlaybackPreferences(): Unit = {
      val p = bindings.inject[PlaybackPreferences](None)
      p.mediaTimeFormat := mediaTimeFormat
      p.notificationButtons := notificationButtons
      p.fastForwardSeconds := fastForwardSeconds
      p.rewindSeconds := rewindSeconds
    }

    applytoUiPreferences()
    applyToPlaybackPreferences()
  }
}

private[sync] object IdentitySettings extends MappingProvider[IdentitySettings] {
  
  def apply(bindings: BindingModule): IdentitySettings = {
    val ui = bindings.inject[UiPreferences](None)
    val playback = bindings.inject[PlaybackPreferences](None)
    apply(
      ui.hideNewInLibrary,
      ui.autoAddToPlaylist,
      ui.skipNew,
      ui.showMediaTypeFilter,
      playback.mediaTimeFormat,
      playback.notificationButtons,
      playback.fastForwardSeconds,
      playback.rewindSeconds
    )
  }

  import mobi.upod.data.Mapping._

  val mapping: Mapping[IdentitySettings] = map(
    "hideNewInLibrary" -> boolean,
    "autoAddToPlaylist" -> boolean,
    "skipNew" -> boolean,
    "showMediaTypeFilter" -> boolean,
    "mediaTimeFormat" -> enumerated(MediaPositionFormat),
    "notificationButtons" -> enumerated(PlaybackNotificationButtons),
    "fastForwardSeconds" -> int,
    "rewindSeconds" -> int
  )(apply)(unapply)
}