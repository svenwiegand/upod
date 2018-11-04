package mobi.upod.app.gui.chapters

import java.io.File

import android.app.Activity
import android.content.{Context, Intent}
import android.os.Bundle
import android.support.v7.app.ActionBarActivity
import android.widget.ImageView
import mobi.upod.android.app.UpNavigation
import mobi.upod.android.content.IntentHelpers.RichIntent
import mobi.upod.android.logging.Logging
import mobi.upod.android.os.{BundleSerializableValue, BundleStringValue}
import mobi.upod.android.view.Helpers.RichView
import mobi.upod.app.{IntentExtraKey, R}
import mobi.upod.media.MediaChapter

class ChapterImageActivity extends ActionBarActivity with UpNavigation with Logging {
  import ChapterImageActivity._

  private lazy val chapter = getIntent.getExtra(Chapter)
  private lazy val mediaFilePath = getIntent.getExtra(MediaFilePath)

  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.chapter_image)
    if (chapter.isEmpty || mediaFilePath.isEmpty)
      finish()

    getSupportActionBar.setTitle(chapter.flatMap(_.title).getOrElse(""))
    showImage()
  }

  override protected def navigateUp(): Unit =
    finish()

  override def finish(): Unit = {
    super.finish()
    overridePendingTransition(0, R.anim.fade_out)
  }

  private def showImage(): Unit = {
    ChapterImageLoader.asyncLoadImage(
      new File(mediaFilePath.get),
      chapter.get.image.get,
      findViewById(R.id.chapterImage).asInstanceOf[ImageView].setImageBitmap,
      showError
    )
  }

  private def showError(error: Throwable): Unit =
    findViewById(R.id.errorMessage).show()
}

object ChapterImageActivity {
  private object Chapter extends BundleSerializableValue[MediaChapter](IntentExtraKey("chapter"))
  private object MediaFilePath extends BundleStringValue(IntentExtraKey("mediaFilePath"))

  def start(context: Context, chapter: MediaChapter, mediaFile: File): Unit = if (chapter.image.nonEmpty && mediaFile.exists) {
    val intent = new Intent(context, classOf[ChapterImageActivity])
    intent.putExtra(Chapter, chapter)
    intent.putExtra(MediaFilePath, mediaFile.getAbsolutePath)
    context.startActivity(intent)
    context match {
      case a: Activity => a.overridePendingTransition(R.anim.fade_in, 0)
      case _ =>
    }
  }
}