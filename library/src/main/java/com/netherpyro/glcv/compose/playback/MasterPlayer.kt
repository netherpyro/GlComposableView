package com.netherpyro.glcv.compose.playback

import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import android.util.Log
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.PlaybackParameters
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.Timeline
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.TrackSelectionArray

/**
 * @author mmikhailov on 11.05.2020.
 *
 * MasterPlayer plays silence that has duration of project's. Needed for listening playback events.
 */
internal class MasterPlayer(
        private val context: Context,
        private val listener: PlaybackEventListener
) {
    private val playbackThread = PlaybackThread()

    init {
        playbackThread.start()
        playbackThread.requestPrepare()
    }

    fun play() {
        playbackThread.requestPlay()
    }

    fun pause() {
        playbackThread.requestPause()
    }

    fun seek(value: Long) {
        playbackThread.requestSeek(value)
    }

    fun release() {
        playbackThread.requestRelease()
    }

    fun setProjectDuration(value: Long) {
        playbackThread.requestInvalidate(value)
    }

    @Suppress("PrivatePropertyName")
    private inner class PlaybackThread : HandlerThread("MasterPlayer_PlaybackThread"), Handler.Callback {

        private val TAG = name
        private val PREPARE = 0
        private val RELEASE = 1
        private val INVALIDATE = 3
        private val PLAY = 4
        private val PAUSE = 5
        private val SEEK = 6

        private val SILENCE_TAG = "SILENCE_TAG"

        private lateinit var handler: Handler
        private lateinit var player: SimpleExoPlayer
        private lateinit var playerDataSource: TaggedSilenceMediaSource

        fun requestPrepare() = handler.sendEmptyMessage(PREPARE)
        fun requestRelease() = handler.sendEmptyMessage(RELEASE)
        fun requestInvalidate(value: Long) = handler.sendMessage(handler.obtainMessage(INVALIDATE, value))
        fun requestPlay() = handler.sendEmptyMessage(PLAY)
        fun requestPause() = handler.sendEmptyMessage(PAUSE)
        fun requestSeek(value: Long) = handler.sendMessage(handler.obtainMessage(SEEK, value))

        @Synchronized
        override fun start() {
            super.start()
            handler = Handler(looper, this)
        }

        override fun handleMessage(msg: Message): Boolean {
            if (!isAlive) {
                Log.d(TAG, "dead thread... Cannot proceed")
                return false
            }

            return when (msg.what) {
                PREPARE -> prepare()
                RELEASE -> release()
                INVALIDATE -> invalidatePlayerSource(msg.obj as Long)
                PLAY -> playWhenReady(true)
                PAUSE -> playWhenReady(false)
                SEEK -> seek(msg.obj as Long)
                else -> false
            }
        }

        private fun playWhenReady(play: Boolean): Boolean {
            player.playWhenReady = play

            return true
        }

        private fun seek(value: Long): Boolean {
            player.seekTo(value)

            return true
        }

        private fun prepare(): Boolean {
            player = SimpleExoPlayer.Builder(context)
                .build()
                .apply {
                    addListener(object : Player.EventListener {
                        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                            if (playbackState == Player.STATE_ENDED) {
                                listener.onPlaybackEnded()
                            }
                        }

                        override fun onIsPlayingChanged(isPlaying: Boolean) {
                            listener.onIsPlayingChanged(isPlaying)
                        }

                        override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {}
                        override fun onSeekProcessed() {}
                        override fun onTracksChanged(tg: TrackGroupArray, ts: TrackSelectionArray) {}
                        override fun onPlayerError(error: ExoPlaybackException) {}
                        override fun onLoadingChanged(isLoading: Boolean) {}
                        override fun onPositionDiscontinuity(reason: Int) {}
                        override fun onRepeatModeChanged(repeatMode: Int) {}
                        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {}
                        override fun onPlaybackSuppressionReasonChanged(playbackSuppressionReason: Int) {}
                        override fun onTimelineChanged(timeline: Timeline, reason: Int) {}
                        override fun onTimelineChanged(timeline: Timeline, manifest: Any?, reason: Int) {}
                    })
                }

            invalidatePlayerSource(0L)

            return true
        }

        private fun invalidatePlayerSource(projectDurationMs: Long): Boolean {
            playerDataSource = TaggedSilenceMediaSource(projectDurationMs.times(1000), SILENCE_TAG)
                .apply { setPtsListener { listener.onProgress(it / 1000L) } }

            player.prepare(playerDataSource)

            return true
        }

        private fun release(): Boolean {
            player.stop()
            player.release()

            return true
        }
    }
}