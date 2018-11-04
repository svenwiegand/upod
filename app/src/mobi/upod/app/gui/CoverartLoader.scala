package mobi.upod.app.gui

import java.io.File
import java.net.URL

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
import com.escalatesoft.subcut.inject.{BindingModule, Injectable}
import com.nostra13.universalimageloader.cache.disc.impl.FileCountLimitedDiscCache
import com.nostra13.universalimageloader.cache.memory.impl.LruMemoryCache
import com.nostra13.universalimageloader.core.assist.{FailReason, ImageLoadingListener}
import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer
import com.nostra13.universalimageloader.core.download.BaseImageDownloader
import com.nostra13.universalimageloader.core.{DisplayImageOptions, ImageLoader, ImageLoaderConfiguration}
import com.nostra13.universalimageloader.utils.StorageUtils
import mobi.upod.android.logging.Logging
import mobi.upod.android.view.DisplayMetrics
import mobi.upod.app.App
import mobi.upod.app.storage.{CoverartProvider, ImageSize}
import mobi.upod.net.UrlEncodableString
import mobi.upod.util.StorageSize.IntStorageSize

class CoverartLoader(implicit val bindingModule: BindingModule) extends Injectable with Logging {
  private lazy val coverartProvider = inject[CoverartProvider]
  private lazy val imageLoader = initImageLoader()
  private implicit lazy val displayMetrics = DisplayMetrics(inject[App])

  private lazy val baseDisplayImageOptionsBuilder = new DisplayImageOptions.Builder()
    .displayer(new FadeInBitmapDisplayer(500))
    .cacheInMemory(true)
  private lazy val localDefaultDisplayImageOptions = baseDisplayImageOptionsBuilder.build()
  private lazy val localListDisplayImageOptions = baseDisplayImageOptionsBuilder.cacheInMemory(true).build()
  private lazy val onlineDisplayImageOptions = baseDisplayImageOptionsBuilder.cacheOnDisc(true).build()

  private def initImageLoader(): ImageLoader = {
    val app = inject[App]
    val cacheDir = new File(StorageUtils.getCacheDirectory(app), "coverart")
    val config = new ImageLoaderConfiguration.Builder(app)
      .imageDownloader(new BaseImageDownloader(app))
      .defaultDisplayImageOptions(localDefaultDisplayImageOptions)
      .memoryCache(new LruMemoryCache(1.mb))
      .discCache(new FileCountLimitedDiscCache(cacheDir, 1000))
      .build()

    val imageLoader = ImageLoader.getInstance
    imageLoader.init(config)
    imageLoader
  }

  def displayImage(view: ImageView, size: ImageSize, url: Option[URL], fallback: Option[CoverartLoaderFallbackDrawable] = None): Unit = url match {
    case Some(u) if u.toString.nonEmpty => displayImage(view, size, u, fallback)
    case _ => fallback.foreach(view.setImageDrawable)
  }

  def displayImage(view: ImageView, size: ImageSize, url: URL, fallback: Option[CoverartLoaderFallbackDrawable]): Unit = {

    def scaledImageUrl(url: URL): String = {
      val encodedUrl = url.toExternalForm.urlEncoded
      val width = size.size.toPx
      s"https://images1-focus-opensocial.googleusercontent.com/gadgets/proxy?container=focus&url=$encodedUrl&resize_w=$width"
    }

    val (uri, local) =
      coverartProvider.getExistingImageFile(url, size).map("file://" + _.getAbsolutePath -> true).getOrElse(scaledImageUrl(url) -> false)

    try {
      val displayOptions = (local, size) match {
        case (true, ImageSize.list) => localListDisplayImageOptions
        case (true, _) => localDefaultDisplayImageOptions
        case (false, _) => onlineDisplayImageOptions
      }
      if (!local) {
        // we're going to load an online image which may take a while, so draw a placeholder if available
        fallback.foreach(view.setImageDrawable)
      }
      imageLoader.displayImage(uri, view, displayOptions, fallback.orNull)
    } catch {
      case _: OutOfMemoryError =>
        log.error(s"failed to display image $uri due to OutOfMemoryError")
    }
  }

  private class FallbackDrawable(drawable: Drawable) extends ImageLoadingListener {
    override def onLoadingStarted(imageUri: String, view: View): Unit = {}

    override def onLoadingCancelled(imageUri: String, view: View): Unit = {}

    override def onLoadingFailed(imageUri: String, view: View, failReason: FailReason): Unit = view match {
      case v: ImageView => v.setImageDrawable(drawable)
      case _ => // ignore
    }

    override def onLoadingComplete(imageUri: String, view: View, loadedImage: Bitmap): Unit = {}
  }
}
