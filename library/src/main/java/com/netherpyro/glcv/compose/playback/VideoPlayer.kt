package com.netherpyro.glcv.compose.playback

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import android.util.Log
import android.view.Surface
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.Timeline
import com.google.android.exoplayer2.source.ClippingMediaSource
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import com.netherpyro.glcv.SurfaceConsumer

/**
 * @author mmikhailov on 04.05.2020.
 */
internal class VideoPlayer(
        private val context: Context,
        private val tag: String,
        private val src: Uri,
        private var startDelayMs: Long,
        private var beginClipAmountMs: Long,
        private var trimmedDuration: Long,
        private var projectDuration: Long,
        private val shouldDrawChangedListener: (Boolean) -> Unit
) : SurfaceConsumer {

    private val playbackThread = PlaybackThread()

    init {
        playbackThread.start()
        playbackThread.requestPrepare()
    }

    override fun consume(surface: Surface) {
        playbackThread.requestSetSurface(surface)
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
        this.projectDuration = value
        playbackThread.requestInvalidate()
    }

    fun setStartDelay(value: Long) {
        this.startDelayMs = value
        playbackThread.requestInvalidate()
    }

    fun setBeginClip(value: Long) {
        this.beginClipAmountMs = value
        playbackThread.requestInvalidate()
    }

    fun setTrimmedDuration(value: Long) {
        this.trimmedDuration = value
        playbackThread.requestInvalidate()
    }

    @Suppress("PrivatePropertyName")
    private inner class PlaybackThread : HandlerThread("${tag}_PlaybackThread"), Handler.Callback {

        private val TAG = name
        private val PREPARE = 0
        private val RELEASE = 1
        private val SET_SURFACE = 2
        private val INVALIDATE = 3
        private val PLAY = 4
        private val PAUSE = 5
        private val SEEK = 6

        private val SILENCE_TAG = "SILENCE_TAG"

        private lateinit var handler: Handler
        private lateinit var player: SimpleExoPlayer
        private lateinit var playerDataSource: ConcatenatingMediaSource
        private lateinit var videoSource: MediaSource

        private var currentlyDrawing = true
        private var nowPositionMs = 0L

        fun requestPrepare() = handler.sendEmptyMessage(PREPARE)
        fun requestRelease() = handler.sendEmptyMessage(RELEASE)
        fun requestSetSurface(surface: Surface) = handler.sendMessage(handler.obtainMessage(SET_SURFACE, surface))
        fun requestInvalidate() = handler.sendEmptyMessage(INVALIDATE)
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
                SET_SURFACE -> setSurface(msg.obj as Surface)
                INVALIDATE -> invalidatePlayerSource()
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
            if (player.currentTimeline.periodCount <= 0) return true

            var seekTime = value
            var periodOrderNum = 0

            val populatedPeriod = player.currentTimeline.getPeriod(periodOrderNum, Timeline.Period())

            while (
                    (populatedPeriod.realDuration() - 1) != Long.MIN_VALUE
                    && seekTime - populatedPeriod.realDuration() > 0
                    && (player.currentTimeline.periodCount) > periodOrderNum + 1
            ) {
                seekTime -= populatedPeriod.realDuration()
                player.currentTimeline.getPeriod(++periodOrderNum, populatedPeriod)
            }

            // minus one to not overlap end bound of period
            seekTime = seekTime.coerceAtMost(populatedPeriod.realDuration() - 1)

            player.seekTo(periodOrderNum, seekTime)

            return true
        }

        private fun prepare(): Boolean {
            playerDataSource = ConcatenatingMediaSource()

            val dataSourceFactory: DataSource.Factory = DefaultDataSourceFactory(
                    context,
                    Util.getUserAgent(context, "GlComposableView")
            )

            videoSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(src)

            player = SimpleExoPlayer.Builder(context)
                .build()
                .apply {
                    invalidatePlayerSource()
                    prepare(playerDataSource)
                    addListener(object : Player.EventListener {
                        override fun onTracksChanged(trackGroups: TrackGroupArray,
                                                     trackSelections: TrackSelectionArray) {
                            if (trackGroups.isSilence()) {
                                Log.i(TAG, "onTracksChanged::went to silence")
                                invalidateDrawing(true)
                            } else {
                                Log.v(TAG, "onTracksChanged::went to video track")
                            }
                        }
                    })
                    setVideoFrameMetadataListener { presentationTimeUs, _, _, _ ->
                        nowPositionMs = startDelayMs + presentationTimeUs / 1000L
                        // Log.d(TAG, "nowPts=$nowPositionMs")
                        invalidateDrawing(false)
                    }
                }

            return true
        }

        private fun setSurface(surface: Surface): Boolean {
            player.setVideoSurface(surface)

            return true
        }

        private fun invalidatePlayerSource(): Boolean {
            playerDataSource.clear()

            val silenceDurationBefore = startDelayMs + beginClipAmountMs
            val silenceDurationAfter = projectDuration - (silenceDurationBefore + trimmedDuration)

            if (silenceDurationBefore > 0) {
                playerDataSource.addMediaSource(
                        TaggedSilenceMediaSource(silenceDurationBefore.times(1000), SILENCE_TAG)
                )
            }

            playerDataSource.addMediaSource(
                    createClippingSource(videoSource, beginClipAmountMs, trimmedDuration)
            )

            if (silenceDurationAfter > 0) {
                playerDataSource.addMediaSource(
                        TaggedSilenceMediaSource(silenceDurationAfter.times(1000), SILENCE_TAG)
                )
            }

            invalidateDrawing(false)

            return true
        }

        private fun invalidateDrawing(forceInvisible: Boolean) {
            if (forceInvisible) {
                Log.w(TAG, "invalidateDrawing::force not drawing")
                currentlyDrawing = false
                shouldDrawChangedListener(currentlyDrawing)
            } else {
                val shouldDrawNow = nowPositionMs in startDelayMs + beginClipAmountMs..(startDelayMs + beginClipAmountMs + trimmedDuration)
                        && nowPositionMs <= projectDuration
                if (shouldDrawNow != currentlyDrawing) {
                    currentlyDrawing = shouldDrawNow
                    Log.d(TAG, "invalidateDrawing::${if (currentlyDrawing) "drawing" else "not drawing"}")
                    shouldDrawChangedListener(currentlyDrawing)
                }
            }
        }

        private fun createClippingSource(ms: MediaSource, startClipMs: Long, endClipMs: Long) =
                ClippingMediaSource(
                        ms,
                        startClipMs * 1000L,
                        endClipMs * 1000L,
                        false,
                        false,
                        true
                )

        private fun release(): Boolean {
            playerDataSource.clear()
            player.stop()
            player.release()

            return true
        }

        private fun Timeline.Period.realDuration(): Long = durationMs + positionInWindowMs

        private fun TrackGroupArray.isSilence() = !this.isEmpty && this[0].getFormat(0).id == SILENCE_TAG
    }
}