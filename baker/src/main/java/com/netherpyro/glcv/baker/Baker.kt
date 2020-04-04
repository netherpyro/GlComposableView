package com.netherpyro.glcv.baker

import android.graphics.Color
import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import android.util.Log
import com.netherpyro.glcv.GlRenderer
import com.netherpyro.glcv.GlViewport
import com.netherpyro.glcv.LayoutHelper
import com.netherpyro.glcv.baker.encode.EncoderConfig
import com.netherpyro.glcv.baker.encode.GlRecoder
import com.netherpyro.glcv.baker.encode.PostRenderCallback
import com.netherpyro.glcv.compose.Composer
import com.netherpyro.glcv.compose.TimeMask

/**
 * @author mmikhailov on 28.03.2020.
 *
 * Bakes (records) composed [Composer.Snapshot] synced with [TimeMask] into video file.
 */
internal class Baker private constructor(
        config: EncoderConfig,
        private val snapshot: Composer.Snapshot,
        private val progressListener: ((progress: Float, completed: Boolean) -> Unit)?
) : Cancellable {

    companion object {
        fun bake(
                snapshot: Composer.Snapshot,
                config: EncoderConfig,
                progressListener: ((progress: Float, completed: Boolean) -> Unit)?
        ): Cancellable = Baker(config, snapshot, progressListener)
    }

    private val frameSyncThread = FrameSyncThread(config)
    private val renderer = GlRenderer(RenderHostStub, Color.BLACK, snapshot.viewportColor)
    private val viewport: GlViewport = LayoutHelper(snapshot.aspectRatio)
        .onSurfaceChanged(config.width, config.height)

    init {
        snapshot.setupWithLayers(renderer)
        frameSyncThread.requestStartRecording()
    }

    override fun cancel() {
        frameSyncThread.requestStop()
    }

    @Suppress("PrivatePropertyName")
    private inner class FrameSyncThread(
            private val encoderConfig: EncoderConfig
    ) : HandlerThread("FrameSyncThread"), Handler.Callback, PostRenderCallback {

        private val TAG = "FrameSyncThread"
        private val START = 0
        private val STOP = 1
        private val FRAME = 2

        private val totalDurationNanos = snapshot.totalDurationMs() * 1_000_000L
        private val frameDurationNanos = 1_000_000_000L / encoderConfig.fps
        private var presentationTimeNanos = -frameDurationNanos //todo minus needed?

        private lateinit var glRecoder: GlRecoder
        private lateinit var handler: Handler

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
                START -> startRecoding()
                STOP -> stopRecoding()
                FRAME -> generateFrame()
                else -> false
            }
        }

        override fun onPostRender() {
            handler.sendEmptyMessage(FRAME)
        }

        fun requestStartRecording() {
            handler.sendEmptyMessage(START)
        }

        fun requestStop() {
            handler.sendEmptyMessage(STOP)
        }

        private fun generateFrame(): Boolean {
            presentationTimeNanos += frameDurationNanos

            snapshot.invalidateLayersVisibility(presentationTimeNanos / 1_000_000)

            // todo play movie frame synchronously if exists

            val progress = presentationTimeNanos / totalDurationNanos.toFloat()
            val hasFrames = presentationTimeNanos <= totalDurationNanos
            progressListener?.invoke(progress, !hasFrames)

            if (hasFrames) {
                glRecoder.frameAvailable(presentationTimeNanos)
            } else {
                stopRecoding()
            }

            return true
        }

        private fun startRecoding(): Boolean {
            glRecoder = GlRecoder(renderer, viewport, encoderConfig, this)
            glRecoder.raiseEncoder()

            generateFrame()

            return true
        }

        private fun stopRecoding(): Boolean {
            glRecoder.stopRecording()

            interrupt()
            quit()

            return true
        }
    }
}