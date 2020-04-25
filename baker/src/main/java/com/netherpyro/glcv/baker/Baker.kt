package com.netherpyro.glcv.baker

import android.content.Context
import android.graphics.Color
import android.opengl.EGLContext
import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import android.util.Log
import android.util.Size
import androidx.annotation.ColorInt
import com.netherpyro.glcv.GlRenderer
import com.netherpyro.glcv.GlViewport
import com.netherpyro.glcv.LayoutHelper
import com.netherpyro.glcv.TransformData
import com.netherpyro.glcv.Transformable
import com.netherpyro.glcv.baker.decode.PassiveDecoderPool
import com.netherpyro.glcv.baker.encode.EncoderConfig
import com.netherpyro.glcv.baker.encode.GlRecorder
import com.netherpyro.glcv.baker.encode.PostRenderCallback
import com.netherpyro.glcv.baker.encode.PrepareCallback
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
        @ColorInt
        val viewportColor: Int,
        val template: Template,
        private val context: Context,
        private val progressListener: ((progress: Float, completed: Boolean) -> Unit)?
) : Cancellable {

    companion object {
        fun bake(
                @ColorInt
                viewportColor: Int,
                template: Template,
                outputPath: String,
                outputMinSidePx: Int,
                fps: Int,
                iFrameIntervalSecs: Int,
                bitRate: Int,
                context: Context,
                eglContext: EGLContext?,
                progressListener: ((progress: Float, completed: Boolean) -> Unit)?
        ): Cancellable {
            val resolution = Util.resolveResolution(template.aspectRatio, outputMinSidePx)
            val config = EncoderConfig(
                    outputPath,
                    resolution.width,
                    resolution.height,
                    fps,
                    iFrameIntervalSecs,
                    bitRate,
                    eglContext
            )

            return Baker(config, viewportColor, template, context, progressListener)
        }
    }

    private val frameSyncThread = BakerThread(config)

    init {
        frameSyncThread.start()
        frameSyncThread.requestStartRecording()
    }

    override fun cancel() {
        frameSyncThread.requestStop()
    }

    @Suppress("PrivatePropertyName", "PropertyName")
    private inner class BakerThread(
            private val config: EncoderConfig
    ) : HandlerThread("BakerThread"), Handler.Callback, PostRenderCallback {

        var VERBOSE = true

        private val TAG = "BakerThread"
        private val START = 0
        private val STOP = 1
        private val FRAME = 2

        private lateinit var handler: Handler
        private lateinit var glRecorder: GlRecorder
        private lateinit var transformables: Array<Transformable>

        private val glRenderer = GlRenderer(RenderHostStub, Color.BLACK, viewportColor)

        private val viewport: GlViewport = LayoutHelper(template.aspectRatio)
            .onSurfaceChanged(config.width, config.height)

        private val decoders = PassiveDecoderPool()

        private val timeMask = TimeMask.from(template.units)
        private val totalDurationNanos = timeMask.durationMs * 1_000_000L
        private val frameDurationNanos = 1_000_000_000L / config.fps

        private var presentationTimeNanos = 0L

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
            setupGlLayers()

            glRecorder = GlRecorder(glRenderer, viewport, config, this, object : PrepareCallback {
                override fun onPrepared() {
                    generateFrame()
                }
            })

            glRecorder.raiseEncoder()

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
            transformables = Array(template.units.size) { index ->
                template.units[index].let { unit ->
                    val metadata = Util.getMetadata(
                            context,
                            unit.uri,
                            defaultImageDuration = Constant.DEFAULT_IMAGE_DURATION_MS
                    )

                    @Suppress("UnnecessaryVariable")
                    val transformable = when (metadata.type) {
                        Type.VIDEO -> glRenderer.addSurfaceLayer(
                                unit.tag,
                                surfaceConsumer = decoders.createSurfaceConsumer(unit.tag, context, unit.uri),
                                position = unit.zPosition,
                                initialValues = TransformData(
                                        scale = unit.scaleFactor,
                                        rotation = unit.rotationDeg,
                                        xFactor = unit.translateFactorX,
                                        yFactor = unit.translateFactorY,
                                        opacity = unit.opacity,
                                        layerSize = Size(metadata.width, metadata.height)
                                )
                        )
                        Type.IMAGE -> glRenderer.addBitmapLayer(
                                unit.tag,
                                bitmap = Util.getBitmap(context, unit.uri),
                                position = unit.zPosition,
                                initialValues = TransformData(
                                        scale = unit.scaleFactor,
                                        rotation = if (unit.tag == "image1") 37f else if (unit.tag == "image2") 90f else 180f,//unit.rotationDeg, // todo restore
                                        xFactor = unit.translateFactorX,
                                        yFactor = unit.translateFactorY,
                                        opacity = if (unit.tag == "image1") 0.5f else if (unit.tag == "image2") 0.3f else 1f//unit.opacity // todo restore
                                )
                        )
                    }

                    return@let transformable
                }
            }
        }

        private fun generateFrame(): Boolean {
            presentationTimeNanos += frameDurationNanos

            val status = timeMask.takeVisibilityStatus(presentationTimeNanos / 1_000_000)

            if (VERBOSE)
                Log.i(TAG,
                        "generateFrame::pTime=${presentationTimeNanos / 1_000_000} ms, statuses=${status.toTypedArray()
                            .contentToString()}")

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
