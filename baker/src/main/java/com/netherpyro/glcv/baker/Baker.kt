package com.netherpyro.glcv.baker

import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import android.util.Log
import androidx.annotation.ColorInt
import com.netherpyro.glcv.GlRenderer
import com.netherpyro.glcv.GlViewport
import com.netherpyro.glcv.LayoutHelper
import com.netherpyro.glcv.Transformable
import com.netherpyro.glcv.baker.decode.DecoderPool
import com.netherpyro.glcv.baker.encode.EncoderConfig
import com.netherpyro.glcv.baker.encode.GlRecoder
import com.netherpyro.glcv.baker.encode.PostRenderCallback
import com.netherpyro.glcv.compose.media.Constant
import com.netherpyro.glcv.compose.media.Type
import com.netherpyro.glcv.compose.media.Util
import com.netherpyro.glcv.compose.template.Template
import com.netherpyro.glcv.compose.template.TimeMask

/**
 * @author mmikhailov on 28.03.2020.
 *
 * Bakes (records) composed [Template] synced with [TimeMask] into video file.
 */
internal class Baker private constructor(
        config: EncoderConfig,
        private val context: Context,
        private val data: BakerData,
        private val progressListener: ((progress: Float, completed: Boolean) -> Unit)?
) : Cancellable {

    companion object {
        fun bake(context: Context,
                 data: BakerData,
                 config: EncoderConfig,
                 progressListener: ((progress: Float, completed: Boolean) -> Unit)?
        ): Cancellable = Baker(config, context, data, progressListener)
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

        private val glRenderer = GlRenderer(RenderHostStub, Color.BLACK, data.viewportColor)
        private val viewport: GlViewport = LayoutHelper(data.template.aspectRatio)
            .onSurfaceChanged(config.width, config.height)

        private val timeMask = data.template.timeMask
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
            setupGlLayers()

            generateFrame()

            return true
        }

        private fun stopRecoding(): Boolean {
            glRecoder.stopRecording()

            interrupt()
            quit()

            return true
        }

        private fun setupGlLayers() {
            transformables = Array(data.template.units.size) { index ->
                data.template.units[index].let { unit ->
                    val metadata = Util.getMetadata(context, unit.uri, Constant.DEFAULT_IMAGE_DURATION_MS)
                    val transformable = when (metadata.type) {
                        Type.VIDEO -> glRenderer.addSurfaceLayer(
                                unit.tag,
                                surfaceConsumer = decoders.createDecoderForTag(unit.tag, context, unit.uri),
                                position = unit.zPosition
                        )
                        Type.IMAGE -> glRenderer.addBitmapLayer(
                                unit.tag,
                                bitmap = Util.getBitmap(context, unit.uri),
                                position = unit.zPosition
                        )
                    }

                    return@let transformable.apply {
                        setScale(unit.scaleFactor)
                        setRotation(unit.rotationDeg)
                        setTranslationFactor(unit.translateFactorX, unit.translateFactorY)
                        setOpacity(unit.opacity)

                        if (metadata.type == Type.VIDEO) {
                            setSize(metadata.width, metadata.height)
                        }
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

data class BakerData(
        @ColorInt
        val viewportColor: Int,
        val template: Template
)
