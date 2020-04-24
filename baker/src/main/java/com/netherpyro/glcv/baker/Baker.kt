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
import com.netherpyro.glcv.baker.decode.PassiveDecoderPool
import com.netherpyro.glcv.baker.encode.EncoderConfig
import com.netherpyro.glcv.baker.encode.GlRecorder
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
        private lateinit var glRecorder: GlRecorder
        private lateinit var transformables: Array<Transformable>

        private val glRenderer = GlRenderer(RenderHostStub, Color.BLACK, data.viewportColor)
        private val viewport: GlViewport = LayoutHelper(data.template.aspectRatio)
            .onSurfaceChanged(config.width, config.height)

        private val timeMask = TimeMask.from(data.template.units)
        private val totalDurationNanos = timeMask.durationMs * 1_000_000L
        private val frameDurationNanos = 1_000_000_000L / config.fps

        private var presentationTimeNanos = -frameDurationNanos //todo minus needed?

        private val decoders = PassiveDecoderPool()

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
            glRecorder = GlRecorder(glRenderer, viewport, config, this)
            glRecorder.raiseEncoder()
            setupGlLayers()

            generateFrame()

            return true
        }

        private fun stopRecoding(): Boolean {
            glRecorder.stopRecording()
            decoders.release()

            interrupt()
            quit()

            return true
        }

        private fun setupGlLayers() {
            transformables = Array(data.template.units.size) { index ->
                data.template.units[index].let { unit ->
                    val metadata = Util.getMetadata(
                            context,
                            unit.uri,
                            defaultImageDuration = Constant.DEFAULT_IMAGE_DURATION_MS
                    )

                    val transformable = when (metadata.type) {
                        Type.VIDEO -> glRenderer.addSurfaceLayer(
                                unit.tag,
                                surfaceConsumer = decoders.createSurfaceConsumer(unit.tag, context, unit.uri),
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

            val status = timeMask.takeVisibilityStatus(presentationTimeNanos / 1_000_000)
            invalidateLayersVisibility(status)
            decoders.advance(status)

            val progress = presentationTimeNanos / totalDurationNanos.toFloat()
            val hasFrames = presentationTimeNanos <= totalDurationNanos
            progressListener?.invoke(progress, !hasFrames)

            if (hasFrames) {
                glRecorder.frameAvailable(presentationTimeNanos)
            } else {
                stopRecoding()
            }

            return true
        }

        private fun invalidateLayersVisibility(statuses: List<TimeMask.VisibilityStatus>) {
            statuses.forEachIndexed { index, status -> transformables[index].setSkipDraw(!status.visible) }
        }
    }
}

data class BakerData(
        @ColorInt
        val viewportColor: Int,
        val template: Template
)
