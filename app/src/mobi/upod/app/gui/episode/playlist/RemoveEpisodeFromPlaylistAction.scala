package mobi.upod.app.gui.episode.playlist

import com.escalatesoft.subcut.inject.BindingModule
import mobi.upod.app.data.EpisodeListItem
import mobi.upod.app.gui.episode.{EpisodeUpdate, AsyncEpisodeAction}
import mobi.upod.app.services.playback.PlaybackService
import android.content.Context

private[episode] abstract class RemoveEpisodeFromPlaylistAction(episode: => Option[EpisodeListItem])(implicit bindings: BindingModule)
  extends AsyncEpisodeAction(episode) with EpisodeUpdate {
     private lazy val playService = inject[PlaybackService]

     protected def enabled(episode: EpisodeListItem) =
       playService.canRemoveAtLeastOneOf(Traversable(episode))

     protected def processData(context: Context, data: EpisodeListItem) = {
       if (playService.removeEpisode(data))
         data.copy(playbackInfo = data.playbackInfo.copy(listPosition = None))
       else
         data
     }
   }
