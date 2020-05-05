package com.netherpyro.glcv.compose

import android.net.Uri
import android.opengl.EGL14
import android.opengl.EGLContext
import android.util.Log
import androidx.annotation.ColorInt
import com.netherpyro.glcv.GlComposableView
import com.netherpyro.glcv.Transformable
import com.netherpyro.glcv.compose.media.Constant
import com.netherpyro.glcv.compose.media.Type
import com.netherpyro.glcv.compose.media.Util
import com.netherpyro.glcv.compose.playback.PlaybackController
import com.netherpyro.glcv.compose.playback.ProjectDurationHolder
import com.netherpyro.glcv.compose.template.Template
import java.util.concurrent.Exchanger

/**
 * @author mmikhailov on 28.03.2020.
 *
 * Wrapper around GlComposableView with timing capabilities
 */
class Composer {

    companion object {
        private const val TAG = "Composer"
    }

    @ColorInt
    var viewportColor: Int = 0
        private set

    @ColorInt
    var baseColor: Int = 0
        private set

    var aspectRatio: Float = 1f
        private set

    private var glView: GlComposableView? = null

    private val mediaSeqs = mutableSetOf<Sequence>()
    private val transformables = mutableSetOf<Transformable>()
    private val projectDurationHolder = ProjectDurationHolder()
    private val playbackController = PlaybackController(projectDurationHolder)

    /**
     * @param glComposableView
     * @param onTransformable lambda callback for providing transformable previously set.
     * Nothing provides if this is first attempt of GlComposableView setting.
     * @return [Controllable] list
     * */
    fun setGlView(glComposableView: GlComposableView, onTransformable: (Transformable) -> Unit): List<Controllable> {
        glView = glComposableView

        glComposableView.setViewportColor(viewportColor)
        glComposableView.setBaseColor(baseColor)
        glComposableView.setAspectRatio(aspectRatio)

        return try {
            applyTemplate(takeTemplate(), onTransformable)
        } catch (e: IllegalStateException) {
            Log.i(TAG, "setGlView::this is first attempt setting GlComposableView")
            emptyList()
        }
    }

    fun setAspectRatio(aspect: Float, animated: Boolean = false) {
        this.aspectRatio = aspect
        glView?.setAspectRatio(aspect, animated)
    }

    fun setBaseColor(@ColorInt color: Int) {
        this.baseColor = color
        glView?.setBaseColor(color)
    }

    fun setViewportColor(@ColorInt color: Int) {
        this.viewportColor = color
        glView?.setViewportColor(color)
    }

    @Suppress("LiftReturnOrAssignment")
    fun getSharedEglContext(): EGLContext? {
        if (glView != null) {
            val exchanger = Exchanger<EGLContext>()
            glView?.enqueueEvent(Runnable { exchanger.exchange(EGL14.eglGetCurrentContext()) })

            return exchanger.exchange(null)

        } else return null
    }

    fun takeTemplate(): Template {
        if (transformables.isEmpty()) {
            throw IllegalStateException(
                    "Cannot take template without GL layers added. Use Composer#addMedia method to add a layer"
            )
        }

        return Template.from(aspectRatio, mediaSeqs, transformables)
    }

    fun applyTemplate(template: Template, onTransformable: (Transformable) -> Unit): List<Controllable> {
        checkGlView("applyTemplate") {
            mediaSeqs.clear()
            transformables.clear()

            template.units
                .sortedBy { it.zPosition }
                .forEach {
                    addMedia(
                            tag = it.tag,
                            src = it.uri,
                            zOrderDirection = ZOrderDirection.TOP,
                            startMs = it.startDelayMs,
                            trimmedDuration = it.trimmedDurationMs,
                            mutedAudio = it.mutedAudio,
                            onTransformable = { transformable -> onTransformable(transformable) }
                    )

                    // todo apply transformations
                }

            return getControllableList()
        }

        return emptyList()
    }

    fun addMedia(
            tag: String,
            src: Uri,
            zOrderDirection: ZOrderDirection = ZOrderDirection.TOP,
            startMs: Long = 0,
            beginClipAmountMs: Long? = null,
            trimmedDuration: Long? = null,
            mutedAudio: Boolean = true, // todo return false after resolving audio issues
            onTransformable: ((Transformable) -> Unit)? = null
    ): Controllable? {
        checkGlView("addMedia") {
            val view = glView!!
            val metadata = Util.getMetadata(view.context, src)

            if (metadata.type != Type.VIDEO && metadata.type != Type.IMAGE) {
                Log.e(TAG, "addMedia::provided URI is neither an video nor audio file identifier")
                return null
            }

            return when (metadata.type) {
                Type.VIDEO -> addVideo(tag, src, zOrderDirection, startMs, beginClipAmountMs, trimmedDuration,
                        mutedAudio, onTransformable)
                Type.IMAGE -> addImage(tag, src, zOrderDirection, startMs,
                        trimmedDuration ?: Constant.DEFAULT_IMAGE_DURATION_MS, onTransformable)
            }
        }

        return null
    }

    /**
     * Adds an image to project
     *
     * @param tag is a unique identifier for added image'
     * @param src is a CONTENT URI of image file;
     * @param zOrderDirection direction of z-order at layer list to add the image;
     * @param startMs specifies when image should start display at project timeline. In milliseconds. Cannot be < 0;
     * @param durationMs of image visibility at project timeline. In milliseconds. Cannot be < 1000;
     * */
    fun addImage(
            tag: String,
            src: Uri,
            zOrderDirection: ZOrderDirection = ZOrderDirection.TOP,
            startMs: Long = 0L,
            durationMs: Long = Constant.DEFAULT_IMAGE_DURATION_MS,
            onTransformable: ((Transformable) -> Unit)? = null
    ): Controllable? {
        checkGlView("addImage") {
            val view = glView!!
            val metadata = Util.getMetadata(view.context, src)

            if (metadata.type != Type.IMAGE) {
                Log.e(TAG, "addImage::provided URI is not an image file identifier")
                return null
            }

            val appliedStartDelaysMs = startMs.coerceAtLeast(0L)
            val appliedDurationMs = durationMs.coerceAtLeast(1000L)
            projectDurationHolder.newSequence(appliedStartDelaysMs, 0L, appliedDurationMs)

            view.addBitmapLayer(
                    tag,
                    bitmap = Util.getBitmap(view.context, src),
                    position = zOrderDirection.toGlRenderPosition(),
                    onTransformable = {
                        this@Composer.transformables.add(it)
                        onTransformable?.invoke(it)
                    }
            )

            val sequence = Sequence(
                    tag = tag,
                    uri = src,
                    startDelayMs = startMs.coerceAtLeast(0L),
                    durationMs = durationMs.coerceAtLeast(1000L),
                    mutedAudio = true
            )

            mediaSeqs.add(sequence)

            return sequence
        }

        return null
    }

    fun addVideo(
            tag: String,
            src: Uri,
            zOrderDirection: ZOrderDirection = ZOrderDirection.TOP,
            startMs: Long = 0,
            beginClipAmountMs: Long? = null,
            trimmedDuration: Long? = null,
            mutedAudio: Boolean = true, // todo return false after resolving audio issues
            onTransformable: ((Transformable) -> Unit)?
    ): Controllable? {
        checkGlView("addVideo") {
            val view = glView!!
            val metadata = Util.getMetadata(view.context, src)

            if (metadata.type != Type.VIDEO) {
                Log.e(TAG, "addVideo::provided URI is not an video file identifier")
                return null
            }

            val appliedStartDelaysMs = startMs.coerceAtLeast(0L)
            val appliedBeginClipAmountMs = beginClipAmountMs?.coerceIn(0L, metadata.durationMs) ?: 0L
            val appliedDurationMs = trimmedDuration?.coerceIn(1000L, metadata.durationMs) ?: metadata.durationMs

            projectDurationHolder.newSequence(appliedStartDelaysMs, appliedBeginClipAmountMs, appliedDurationMs)

            view.addSurfaceLayer(
                    tag,
                    surfaceConsumer = playbackController.createPlayer(
                            view.context, tag, src, appliedStartDelaysMs, appliedBeginClipAmountMs, appliedDurationMs
                    ),
                    position = zOrderDirection.toGlRenderPosition(),
                    onTransformable = {
                        it.setSize(metadata.width, metadata.height)
                        this@Composer.transformables.add(it)
                        onTransformable?.invoke(it)
                    }
                )

            val sequence = Sequence(
                    tag = tag,
                    uri = src,
                    startDelayMs = appliedStartDelaysMs,
                    durationMs = appliedDurationMs,
                    mutedAudio = mutedAudio or metadata.hasAudio.not()
            )

            mediaSeqs.add(sequence)

            return sequence
        }

        return null
    }

    fun removeMedia(tag: String) {
        checkGlView("removeMedia") {
            // todo modify sequence list
            // todo apply to glView
        }
    }

    fun moveMediaLayerStepForward(tag: String) {
        checkGlView("moveMediaLayerStepForward") {
            // todo modify sequence list
            // todo apply to glView
        }
    }

    fun moveMediaLayerStepBackwards(tag: String) {
        checkGlView("moveMediaLayerStepBackwards") {
            // todo modify sequence list
            // todo apply to glView
        }
    }

    fun getControllableList(): List<Controllable> = mediaSeqs.toList()

    private inline fun checkGlView(op: String, block: () -> Unit) {
        if (glView == null) {
            Log.e(TAG, "$op::Cannot perform action without GlComposableView set")
            return
        }

        block()
    }
}
