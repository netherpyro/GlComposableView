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
        context: Context,
        private val durationHolder: ProjectDurationHolder,
        private val changeLayerVisibilityListener: (String, Boolean) -> Unit
) : IPlaybackController {

    private val handler = Handler(Looper.myLooper() ?: Looper.getMainLooper())
    private val playerList = mutableMapOf<String, VideoPlayer>()
    private val masterPlayer: MasterPlayer

    private var isPlaying = false
    private var currentPositionMs: Long = 0L
    private var externalPlaybackEventListener: PlaybackEventListener? = null

    init {
        masterPlayer = MasterPlayer(context, object : PlaybackEventListener {
            override fun onProgress(positionMs: Long) {
                handler.post {
                    currentPositionMs = positionMs
                    // todo invalidate layers visibility here
                    externalPlaybackEventListener?.onProgress(positionMs)
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                handler.post {
                    this@PlaybackController.isPlaying = isPlaying
                    externalPlaybackEventListener?.onIsPlayingChanged(isPlaying)
                }
            }

            override fun onPlaybackEnded() {
                handler.post {
                    externalPlaybackEventListener?.onPlaybackEnded()
                }
            }
        })

        durationHolder.listenChanges { newDuration ->
            masterPlayer.setProjectDuration(newDuration)
            playerList.values.forEach { it.setProjectDuration(newDuration) }
        }
    }

    fun createPlayer(
            context: Context,
            tag: String,
            uri: Uri,
            startMs: Long,
            beginClipAmountMs: Long,
            trimmedDurationMs: Long
    ): SurfaceConsumer {
        val player = VideoPlayer(
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
        masterPlayer.play()
        playerList.values.forEach { it.play() }
    }

    override fun pause() {
        isPlaying = false
        masterPlayer.pause()
        playerList.values.forEach { it.pause() }
    }

    override fun isPlaying(): Boolean = isPlaying

    override fun togglePlay() { if (isPlaying) pause() else play() }

    override fun seek(ms: Long) {
        masterPlayer.seek(ms)
        playerList.values.forEach { it.seek(ms) }
    }

    override fun getCurrentPosition(): Long = currentPositionMs

    override fun release() {
        externalPlaybackEventListener = null
        isPlaying = false
        masterPlayer.release()
        playerList.values.forEach { it.release() }
        playerList.clear()
    }

    override fun setPlaybackEventsListener(listener: PlaybackEventListener?) {
        externalPlaybackEventListener = listener
    }
}