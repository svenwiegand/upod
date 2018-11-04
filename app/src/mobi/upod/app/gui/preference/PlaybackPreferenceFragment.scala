package mobi.upod.app.gui.preference

import mobi.upod.android.preference.SimplePreferenceFragment
import mobi.upod.app.R
import mobi.upod.app.storage.PlaybackPreferences
import mobi.upod.android.util.ApiLevel


class PlaybackPreferenceFragment extends SimplePreferenceFragment(R.xml.pref_playback) with PremiumPreferences {
  protected def prefs = Some(inject[PlaybackPreferences])

  override protected def premiumPreferences: Seq[CharSequence] = Seq(
    "pref_skip_back_chapter"
  )

  override protected def conditionalPreferences = Map(
    "pref_playback_notification_buttons" -> (ApiLevel >= ApiLevel.JellyBean && ApiLevel < ApiLevel.Lollipop),
    "pref_audio_player" -> (ApiLevel >= ApiLevel.JellyBean)
  )
}
