package mobi.upod.app.gui

import mobi.upod.app.IntentExtraKey
import mobi.upod.app.data.PodcastListItem
import mobi.upod.android.os.BundleSerializableValue

object PodcastSelection extends BundleSerializableValue[PodcastListItem](IntentExtraKey("selectedPodcast"))