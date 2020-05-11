package com.netherpyro.glcv.compose.playback

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import com.netherpyro.glcv.SurfaceConsumer

/**
 * @author mmikhailov on 04.05.2020.
 */
internal class PlaybackController(
        private val durationHolder: ProjectDurationHolder,
        private val changeLayerVisibilityListener: (String, Boolean) -> Unit
) : IPlaybackController {
    private val handler = Handler(Looper.myLooper() ?: Looper.getMainLooper())
    private val playerList = mutableMapOf<String, InternalPlayer>()

    private var isPlaying = false

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

    override fun play() {
        isPlaying = true
        playerList.values.forEach { it.play() }
    }

    override fun pause() {
        isPlaying = false
        playerList.values.forEach { it.pause() }
    }

    override fun isPlaying(): Boolean = isPlaying

    override fun togglePlay() { if (isPlaying) pause() else play() }

    override fun seek(ms: Long) { playerList.values.forEach { it.seek(ms) } }

    override fun release() {
        isPlaying = false
        playerList.values.forEach { it.release() }
        playerList.clear()
    }
}