package mobi.upod.app.gui.podcast

import java.net.URI

import android.view.View
import android.widget.{ImageView, TextView}
import com.escalatesoft.subcut.inject.{BindingModule, Injectable}
import mobi.upod.android.content.Theme._
import mobi.upod.android.view.Helpers._
import mobi.upod.android.widget.ViewHolder
import mobi.upod.app.R
import mobi.upod.app.data.PodcastListItem
import mobi.upod.app.gui.{CoverartLoaderFallbackDrawable, CoverartPlaceholderDrawable, CoverartLoader, Theme}
import mobi.upod.app.storage.{UiPreferences, ImageSize}

private[podcast] class PodcastListItemViewHolder(view: View, imageSize: ImageSize, grid: Boolean, slowLoadingImages: Boolean)(implicit val bindingModule: BindingModule)
  extends ViewHolder[PodcastListItem] with Injectable {

  private val uiPreferences = inject[UiPreferences]
  private val coverartLoader = inject[CoverartLoader]
  private var podcastUri: Option[URI] = None

  private val theme = new Theme(view.getContext)
  private val imageView = view.childImageView(R.id.podcastImage)
  private val counterView = view.optionalChildAs[TextView](R.id.episodeCounter)
  private val subscriptionIndicator = view.childImageView(R.id.subscriptionIndicator)
  private val errorIndicator = view.optionalChildAs[ImageView](R.id.errorIndicator)
  private val titleView = view.childAs[TextView](R.id.podcastTitle)
  private val categoriesView = view.optionalChildAs[TextView](R.id.podcastCategories)
  private val coverartPlaceholderDrawable = new CoverartPlaceholderDrawable with CoverartLoaderFallbackDrawable

  def setItem(position: Int, item: PodcastListItem) {
    if (podcastUri.isEmpty || podcastUri.get != item.uri) {
      updateTitle(item)
      updateCategories(item)
      updateImage(item)
    }
    updateCounter(item)
    updateSubscriptionIndicator(item)
    updateErrorIndicator(item)
    podcastUri = Some(item.uri)
  }

  private def updateImage(item: PodcastListItem) {
    coverartPlaceholderDrawable.set(item.title, item.extractedOrGeneratedColors)
    if (slowLoadingImages)
      imageView.setImageDrawable(coverartPlaceholderDrawable)
    else
      imageView.setImageDrawable(null)
    coverartLoader.displayImage(imageView, imageSize, item.imageUrl, Some(coverartPlaceholderDrawable))
  }

  private def updateCounter(item: PodcastListItem): Unit = counterView foreach { view =>
    view.show(item.episodeCount > 0)
    view.setText(item.episodeCount.toString)
    if (grid) {
      view.setTextColor(item.extractedOrGeneratedColors.nonLightBackground)
    }
  }

  private def updateSubscriptionIndicator(item: PodcastListItem) {
    subscriptionIndicator.show(item.subscribed)
  }

  private def updateErrorIndicator(item: PodcastListItem): Unit =
    errorIndicator.foreach(_.show(item.syncError.isDefined))

  private def updateTitle(item: PodcastListItem): Unit = {
    titleView.setText(item.title)
    if (grid) {
      titleView.show(uiPreferences.showPodcastGridTitle)
      if (uiPreferences.showPodcastGridTitle) {
        view.setBackgroundResource(R.drawable.podcast_grid_item_background)
      } else {
        view.setBackgroundColor(view.getContext.getThemeColor(R.attr.coverartBackground))
        view.setPadding(0, 0, 0, 0)
      }
    }
  }

  private def updateCategories(item: PodcastListItem): Unit = {
    val ctx = view.getContext
    val categories = item.categories.map(_.displayString(ctx)).filter(_.nonEmpty).map(_.get).mkString(", ")
    categoriesView.foreach(_.setText(categories))
  }
}
