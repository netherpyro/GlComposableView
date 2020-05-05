package com.netherpyro.glcv.compose.playback

import android.content.Context
import android.net.Uri
import com.netherpyro.glcv.SurfaceConsumer

/**
 * @author mmikhailov on 04.05.2020.
 */
// todo provide control interface
class PlaybackController(private val durationHolder: ProjectDurationHolder) {

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
        ) { wantsDraw -> /* todo */ }

        playerList[tag] = player

        return player
    }
}