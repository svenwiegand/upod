package mobi.upod.app.gui

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
import com.nostra13.universalimageloader.core.assist.{FailReason, ImageLoadingListener}

trait CoverartLoaderFallbackDrawable extends Drawable with ImageLoadingListener {

  override def onLoadingStarted(imageUri: String, view: View): Unit = {}

  override def onLoadingCancelled(imageUri: String, view: View): Unit = {}

  override def onLoadingComplete(imageUri: String, view: View, loadedImage: Bitmap): Unit = {}

  override def onLoadingFailed(imageUri: String, view: View, failReason: FailReason): Unit = view match {
    case v: ImageView => v.setImageDrawable(this)
    case _ => // ignore
  }
}
