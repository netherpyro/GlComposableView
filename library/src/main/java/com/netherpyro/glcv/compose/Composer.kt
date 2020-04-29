package com.netherpyro.glcv.compose

import android.net.Uri
import android.opengl.EGL14
import android.opengl.EGLContext
import android.util.Log
import androidx.annotation.ColorInt
import com.netherpyro.glcv.GlComposableView
import com.netherpyro.glcv.SurfaceConsumer
import com.netherpyro.glcv.Transformable
import com.netherpyro.glcv.compose.media.Constant
import com.netherpyro.glcv.compose.media.Type
import com.netherpyro.glcv.compose.media.Util
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

    fun setGlView(glComposableView: GlComposableView) {
        glView = glComposableView

        glComposableView.setViewportColor(viewportColor)
        glComposableView.setBaseColor(baseColor)
        glComposableView.setAspectRatio(aspectRatio)

        try {
            applyTemplate(takeTemplate())
        } catch (e: IllegalStateException) {
            Log.i(TAG, "setGlView::this is first attempt setting GlComposableView")
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

    fun applyTemplate(template: Template) {
        checkGlView("applyTemplate") {
            mediaSeqs.clear()
            mediaSeqs.addAll(template.toSequences())
            // todo add layers to gl view
        }
    }

    fun addMedia(
            tag: String,
            src: Uri,
            zOrderDirection: ZOrderDirection = ZOrderDirection.TOP,
            startMs: Long = 0,
            trimmedDuration: Long? = null,
            mutedAudio: Boolean = true, // todo return false after resolving audio issues
            onTransformable: (Transformable) -> Unit
    ): Controllable? {
        checkGlView("addMedia") {
            val view = glView!!
            val metadata = Util.getMetadata(view.context, src)

            if (metadata.type != Type.VIDEO && metadata.type != Type.IMAGE) {
                Log.e(TAG, "addMedia::provided URI is neither an video nor audio file identifier")
                return null
            }

            return when (metadata.type) {
                Type.VIDEO -> addVideo(tag, src, zOrderDirection, startMs, trimmedDuration, mutedAudio, onTransformable)
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
            onTransformable: (Transformable) -> Unit
    ): Controllable? {
        checkGlView("addImage") {
            val view = glView!!
            val metadata = Util.getMetadata(view.context, src)

            if (metadata.type != Type.IMAGE) {
                Log.e(TAG, "addImage::provided URI is not an image file identifier")
                return null
            }

            view.addBitmapLayer(
                    tag,
                    bitmap = Util.getBitmap(view.context, src),
                    position = zOrderDirection.toGlRenderPosition(),
                    onTransformable = {
                        this@Composer.transformables.add(it)
                        onTransformable(it)
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
            trimmedDuration: Long? = null,
            mutedAudio: Boolean = true, // todo return false after resolving audio issues
            onTransformable: (Transformable) -> Unit
    ): Controllable? {
        checkGlView("addVideo") {
            val view = glView!!
            val metadata = Util.getMetadata(view.context, src)

            if (metadata.type != Type.VIDEO) {
                Log.e(TAG, "addVideo::provided URI is not an video file identifier")
                return null
            }

            view.addSurfaceLayer(
                    tag,
                    surfaceConsumer = SurfaceConsumer { /* todo use video player */ },
                    position = zOrderDirection.toGlRenderPosition(),
                    onTransformable = {
                        this@Composer.transformables.add(it)
                        onTransformable(it)
                    }
                )

            val sequence = Sequence(
                    tag = tag,
                    uri = src,
                    startDelayMs = startMs.coerceAtLeast(0L),
                    durationMs = trimmedDuration?.coerceAtLeast(1000L) ?: metadata.durationMs,
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

    private inline fun checkGlView(op: String, block: () -> Unit) {
        if (glView == null) {
            Log.e(TAG, "$op::Cannot perform action without GlComposableView set")
            return
        }

        block()
    }
}
