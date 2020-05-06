package com.netherpyro.glcv.compose.playback

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import com.netherpyro.glcv.SurfaceConsumer

/**
 * @author mmikhailov on 04.05.2020.
 */
// todo provide control interface
class PlaybackController(
        private val durationHolder: ProjectDurationHolder,
        private val changeLayerVisibilityListener: (String, Boolean) -> Unit
) {
    private val handler = Handler(Looper.myLooper() ?: Looper.getMainLooper())
    private val playerList = mutableMapOf<String, InternalPlayer>()

    fun createPlayer(
            context: Context,
            tag: String,
            uri: Uri,
            startMs: Long,
            beginClipAmountMs: Long,
            trimmedDurationMs: Long
    ): SurfaceConsumer {
        val player = InternalPlayer(
                context,
                tag,
                uri,
                startMs,
                beginClipAmountMs,
                trimmedDurationMs,
                durationHolder.projectDuration
        ) { wantsDraw -> handler.post { changeLayerVisibilityListener(tag, wantsDraw) } }

        playerList[tag] = player

        return player
    }
}