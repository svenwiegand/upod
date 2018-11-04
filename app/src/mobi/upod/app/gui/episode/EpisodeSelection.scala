package mobi.upod.app.gui.episode

import mobi.upod.android.os.BundleSerializableValue
import mobi.upod.app.{IntentExtraKey}
import mobi.upod.app.data.EpisodeListItem

private[episode] object EpisodeSelection extends BundleSerializableValue[EpisodeListItem](IntentExtraKey("selectedEpisode"))
