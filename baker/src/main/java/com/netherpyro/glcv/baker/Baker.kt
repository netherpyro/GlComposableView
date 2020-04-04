package com.netherpyro.glcv.baker

import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import android.util.Log
import com.netherpyro.glcv.GlRenderer
import com.netherpyro.glcv.GlViewport
import com.netherpyro.glcv.LayoutHelper
import com.netherpyro.glcv.Transformable
import com.netherpyro.glcv.baker.decode.DecoderPool
import com.netherpyro.glcv.baker.encode.EncoderConfig
import com.netherpyro.glcv.baker.encode.GlRecoder
import com.netherpyro.glcv.baker.encode.PostRenderCallback
import com.netherpyro.glcv.compose.Composer
import com.netherpyro.glcv.compose.LayerType
import com.netherpyro.glcv.compose.TimeMask

/**
 * @author mmikhailov on 28.03.2020.
 *
 * Bakes (records) composed [Composer.Snapshot] synced with [TimeMask] into video file.
 */
internal class Baker private constructor(
        config: EncoderConfig,
        private val context: Context,
        private val snapshot: Composer.Snapshot,
        private val progressListener: ((progress: Float, completed: Boolean) -> Unit)?
) : Cancellable {

    companion object {
        fun bake(context: Context,
                 snapshot: Composer.Snapshot,
                 config: EncoderConfig,
                 progressListener: ((progress: Float, completed: Boolean) -> Unit)?
        ): Cancellable = Baker(config, context, snapshot, progressListener)
    }

    private val frameSyncThread = BakerThread(config)

    init {
        frameSyncThread.requestStartRecording()
    }

    override fun cancel() {
        frameSyncThread.requestStop()
    }

    @Suppress("PrivatePropertyName")
    private inner class BakerThread(
            private val config: EncoderConfig
    ) : HandlerThread("BakerThread"), Handler.Callback, PostRenderCallback {

        private val TAG = "BakerThread"
        private val START = 0
        private val STOP = 1
        private val FRAME = 2

        private lateinit var handler: Handler
        private lateinit var glRecoder: GlRecoder
        private lateinit var transformables: Array<Transformable>

        private val glRenderer = GlRenderer(RenderHostStub, Color.BLACK, snapshot.viewportColor)
        private val viewport: GlViewport = LayoutHelper(snapshot.aspectRatio)
            .onSurfaceChanged(config.width, config.height)

        private val timeMask = TimeMask.from(snapshot.layers)
        private val totalDurationNanos = timeMask.durationMs * 1_000_000L
        private val frameDurationNanos = 1_000_000_000L / config.fps

        private var presentationTimeNanos = -frameDurationNanos //todo minus needed?

        private val decoders = DecoderPool()

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

        private fun startRecoding(): Boolean {
            glRecoder = GlRecoder(glRenderer, viewport, config, this)
            glRecoder.raiseEncoder()
            setupGlRenderer()

            generateFrame()

            return true
        }

        private fun stopRecoding(): Boolean {
            glRecoder.stopRecording()

            interrupt()
            quit()

            return true
        }

        private fun setupGlRenderer() {
            transformables = Array(snapshot.layers.size) { index ->
                snapshot.layers[index].run {
                    return@run when (type) {
                        LayerType.VIDEO -> glRenderer.addSurfaceLayer(
                                tag,
                                surfaceConsumer = decoders.createDecoderForTag(tag, context, uri),
                                position = zPosition
                        )
                        LayerType.IMAGE -> glRenderer.addBitmapLayer(
                                tag,
                                bitmap = BitmapProvider.get(context, uri),
                                position = zPosition
                        )
                    }
                }
            }
        }

        private fun generateFrame(): Boolean {
            presentationTimeNanos += frameDurationNanos

            invalidateLayersVisibility(presentationTimeNanos / 1_000_000)

            // todo resolve video's timestamps
            //decoders.advance()

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

        private fun invalidateLayersVisibility(timestampMs: Long) {
            timeMask.takeVisibilityStatus(timestampMs)
                .values.forEachIndexed { index, visible -> transformables[index].setSkipDraw(!visible) }
        }
    }
}