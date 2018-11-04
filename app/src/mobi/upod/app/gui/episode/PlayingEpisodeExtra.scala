package mobi.upod.app.gui.episode

import mobi.upod.android.os.BundleSerializableValue
import mobi.upod.app.IntentExtraKey
import mobi.upod.app.data.{EpisodeBaseWithPlaybackInfo, EpisodeListItem}

object PlayingEpisodeExtra extends BundleSerializableValue[EpisodeListItem](IntentExtraKey("playingEpisode"))
