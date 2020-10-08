package com.netherpyro.glcv.baker

import android.content.Context
import android.graphics.Color
import android.opengl.EGLContext
import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import android.util.Log
import androidx.annotation.ColorInt
import com.netherpyro.glcv.GlRenderer
import com.netherpyro.glcv.GlViewport
import com.netherpyro.glcv.LayoutHelper
import com.netherpyro.glcv.Transformable
import com.netherpyro.glcv.asTransformData
import com.netherpyro.glcv.baker.decode.PassiveDecoderPool
import com.netherpyro.glcv.baker.encode.AudioBufferProvider
import com.netherpyro.glcv.baker.encode.Encoder
import com.netherpyro.glcv.baker.encode.EncoderConfig
import com.netherpyro.glcv.baker.encode.PostRenderCallback
import com.netherpyro.glcv.baker.encode.PrepareCallback
import com.netherpyro.glcv.compose.media.Constant
import com.netherpyro.glcv.compose.media.Type
import com.netherpyro.glcv.compose.media.Util
import com.netherpyro.glcv.compose.template.Template
import com.netherpyro.glcv.compose.template.TimeMask
import java.io.File

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
        private val progressPublisher: BakeProgressPublisher?
) : Cancellable {

    companion object {
        var AUDIO_PROCESSING_BY_FFMPEG = true
        var VERBOSE_LOGGING = false

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
                progressPublisher: BakeProgressPublisher?
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

            return Baker(config, viewportColor, template, context, progressPublisher)
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

        private val TAG = "BakerThread"
        private val START = 0
        private val STOP = 1
        private val FRAME = 2

        private lateinit var handler: Handler
        private lateinit var encoder: Encoder
        private lateinit var transformables: Array<Transformable>
        private lateinit var muteCalculator: MuteCalculator

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
                START -> startEncoding()
                STOP -> stopEncoding()
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

        private fun startEncoding(): Boolean {
            prepareMedia()

            val processAudioByEncoder =
                    if (AUDIO_PROCESSING_BY_FFMPEG) false
                    else muteCalculator.shouldSoundAtLeastOne()

            if (AUDIO_PROCESSING_BY_FFMPEG && muteCalculator.shouldSoundAtLeastOne()) {
                config.tempOutputPath = File(context.cacheDir, "temp_${System.currentTimeMillis()}").absolutePath
            }

            encoder = Encoder(
                    glRenderer,
                    viewport,
                    processAudioByEncoder,
                    config,
                    this,
                    object : PrepareCallback {
                        override fun onPrepared(audioBufferProvider: AudioBufferProvider) {
                            decoders.prepare(audioBufferProvider)
                            generateFrame()
                        }
                    })

            encoder.prepare()

            return true
        }

        private fun stopEncoding(): Boolean {
            encoder.stop()
            decoders.release()

            quitSafely()

            return true
        }

        private fun prepareMedia() {
            muteCalculator = MuteCalculator()
            transformables = Array(template.units.size) { index ->
                template.units[index].let { unit ->
                    val metadata = Util.getMetadata(
                            context,
                            unit.uri,
                            defaultImageDuration = Constant.DEFAULT_IMAGE_DURATION_MS
                    )

                    @Suppress("UnnecessaryVariable")
                    val transformable = when (metadata.type) {
                        Type.VIDEO -> {
                            val hasSound = !muteCalculator.addEntry(unit.tag, unit.mutedAudio, metadata.hasAudio)
                            val decodeSound =
                                    if (AUDIO_PROCESSING_BY_FFMPEG) false
                                    else hasSound

                            glRenderer.addSurfaceLayer(
                                    unit.tag,
                                    surfaceConsumer = decoders.createMediaDecoder(
                                            context,
                                            unit.tag,
                                            unit.uri,
                                            decodeAudioTrack = decodeSound
                                    ),
                                    position = unit.zPosition,
                                    initialValues = unit.asTransformData(metadata.width, metadata.height)
                            )
                        }
                        Type.IMAGE -> glRenderer.addBitmapLayer(
                                unit.tag,
                                bitmap = Util.getBitmap(context, unit.uri),
                                position = unit.zPosition,
                                initialValues = unit.asTransformData()
                        )
                    }

                    return@let transformable
                }
            }
        }

        private fun generateFrame(): Boolean {
            presentationTimeNanos += frameDurationNanos

            val status = timeMask.takeVisibilityStatus(presentationTimeNanos / 1_000_000)

            if (VERBOSE_LOGGING)
                Log.v(TAG, "generateFrame::pTime=${presentationTimeNanos / 1_000_000} ms, " +
                                "statuses=${status.toTypedArray()
                                    .contentToString()}")

            invalidateLayersVisibility(status)

            decoders.advance(presentationTimeNanos / 1_000L, status)
            val hasFrames = presentationTimeNanos <= totalDurationNanos

            if (hasFrames) {
                progressPublisher?.apply {
                    publish(EncodeTarget.VIDEO, presentationTimeNanos / totalDurationNanos.toFloat(), false)
                }

                encoder.frameAvailable(presentationTimeNanos)
            } else {
                handleEndOfFrames()
            }

            return true
        }

        private fun handleEndOfFrames() {
            var encodeTarget = EncodeTarget.VIDEO

            fun tearDown() {
                progressPublisher?.publish(encodeTarget, 1f, true)
                quitSafely()
            }

            encoder.stop()
            decoders.release()

            if (muteCalculator.shouldSoundAtLeastOne()) {
                encodeTarget = EncodeTarget.AUDIO
                val mediaWithSound =
                        template.units.filter { muteCalculator.shouldSound(it.tag) }

                // ffmpeg asynchronous audio merging pipeline
                FFmpegLauncher.putAudio(
                        context = context,
                        targetPath = config.tempOutputPath,
                        resultPath = config.outputPath,
                        mediaUnits = mediaWithSound,
                        onTimeProgress = { time ->
                            val progress: Float = time.toFloat() / timeMask.durationMs
                            progressPublisher?.publish(encodeTarget, progress, false)
                        },
                        onFinish = ::tearDown,
                )
            } else tearDown()
        }

        private fun invalidateLayersVisibility(statuses: List<TimeMask.VisibilityStatus>) {
            statuses.forEachIndexed { index, status -> transformables[index].setSkipDraw(!status.visible) }
        }
    }
}
