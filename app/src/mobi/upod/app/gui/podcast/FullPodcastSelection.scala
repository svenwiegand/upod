package mobi.upod.app.gui.podcast

import mobi.upod.android.os.BundleSerializableValue
import mobi.upod.app.IntentExtraKey
import mobi.upod.app.data.Podcast

private[podcast] object FullPodcastSelection extends BundleSerializableValue[Podcast](IntentExtraKey("fullSelectedPodcast"))
